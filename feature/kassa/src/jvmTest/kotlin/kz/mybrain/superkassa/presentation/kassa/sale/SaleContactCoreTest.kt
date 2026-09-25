package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.TestSms
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Контакт покупателя с экрана продажи — на настоящем ядре с настройками
 * приложения, тестовом БФД и подменном SMS: чек с контактом ставит доставку
 * на этот контакт и доставляется, чек без контакта доставки не ставит,
 * а набранный с ошибкой контакт чек не пробивает.
 *
 * Доставка необязательна: продажа начинается с «не отправлять», и такой
 * чек задач доставки не ставит, даже если телефон уже набирали. Вид,
 * чей канал в настройках ядра не настроен, не выбирается; без каналов
 * выбирать нечего вовсе.
 */
class SaleContactCoreTest {
    private val sms = TestSms().apply { failing = null }
    private val desk = CoreDesk(listOf(sms))

    @AfterTest
    fun close() = desk.close()

    /** Доставка последней продажи смены, как её видит ядро. */
    private fun ReadyKassa.lastDeliveries(): List<ReceiptDeliveryResponse> =
        desk.bench.superkassa.delivery.receiptDeliveries(kkmId, sales().last().id, cashierPin)

    @Test
    fun `чек с телефоном покупателя уходит ему по SMS и доставлен`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Кумыс", "600")
        model.form.contact.kind(ContactKind.Phone)
        model.form.contact.text("8 (701) 765-43-21")

        model.issue()
        assertIs<Message.Done>(desk.said, desk.saidText)
        kassa.deliverReceipts()

        assertEquals(listOf("+77017654321"), sms.sent.map { it.destination })
        assertEquals(ReceiptDeliveryState.DELIVERED, kassa.lastDeliveries().single().state)
        assertTrue(model.state.value.form.contact.empty, "контакт покупателя перешёл в следующий чек")
    }

    @Test
    fun `чек без контакта покупателя доставку не ставит`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Кумыс", "600")

        model.issue()
        assertIs<Message.Done>(desk.said, desk.saidText)
        kassa.deliverReceipts()

        assertTrue(kassa.lastDeliveries().isEmpty(), "доставка поставлена без контакта")
        assertTrue(sms.sent.isEmpty(), "чек без контакта ушёл по SMS")
    }

    @Test
    fun `«не отправлять» — чек без задач доставки, хотя телефон набирали`() {
        val kassa = desk.seated()
        val model = desk.sale()
        assertEquals(ContactKind.None, model.state.value.form.contact.kind, "продажа началась не с «не отправлять»")
        model.add("Кумыс", "600")
        model.form.contact.kind(ContactKind.Phone)
        model.form.contact.text("8 (701) 765-43-21")
        model.form.contact.kind(ContactKind.None)

        model.issue()
        assertIs<Message.Done>(desk.said, desk.saidText)
        kassa.deliverReceipts()

        assertTrue(kassa.lastDeliveries().isEmpty(), "«не отправлять» поставило доставку")
        assertTrue(sms.sent.isEmpty(), "«не отправлять» отправило SMS")
    }

    @Test
    fun `вид с ненастроенным каналом не выбирается`() {
        desk.seated()
        val model = desk.sale()

        model.form.contact.kind(ContactKind.Email)
        model.form.contact.kind(ContactKind.Telegram)

        val state = model.state.value
        assertEquals(setOf(ContactKind.Phone), state.channels.ready, "настроен только SMS — только телефон")
        assertEquals(ContactKind.None, state.form.contact.kind, "выбрался вид без настроенного канала")
    }

    @Test
    fun `контакт с ошибкой — кнопка молчит, в БФД ничего не ушло`() {
        desk.seated()
        val model = desk.sale()
        model.add("Кумыс", "600")
        model.form.contact.kind(ContactKind.Phone)
        model.form.contact.text("8 701 000")

        model.issue()

        assertEquals(SaleBlock.CustomerContact, model.state.value.block)
        assertTrue(desk.bfd.countedTickets().isEmpty())
    }
}
