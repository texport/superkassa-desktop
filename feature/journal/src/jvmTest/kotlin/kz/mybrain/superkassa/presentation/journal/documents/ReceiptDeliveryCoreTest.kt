package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.kassa.TestSms
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Доставка чека покупателю в журнале — на настоящем ядре, тестовом БФД
 * и подменном SMS, как у кассира: чек открыт строкой журнала, повтор —
 * кнопкой окна, итог — в ядре и в канале.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptDeliveryCoreTest {
    private lateinit var scene: DeliveryBench

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        if (::scene.isInitialized) scene.close()
        Dispatchers.resetMain()
    }

    private fun start(ordered: Boolean = true) = DeliveryBench(ordered).also { scene = it }

    /** Журнал за неделю, прочитанный и открытый на чеке [receipt]. */
    private fun opened(receipt: ReceiptResponse): JournalViewModel = scene.model().apply {
        choose(JournalPeriod.of(JournalSpan.Week))
        open(receipt.documentId)
    }

    private val JournalViewModel.delivery: ReceiptDeliveryUi
        get() = assertNotNull(state.value.delivery, "окно доставки не открылось")

    @Test
    fun `не дошедший чек виден с причиной, и повтор доставляет его покупателю`() {
        val sale = start().sale()
        scene.failForGood(sale)
        val model = opened(sale)

        val failed = model.delivery.deliveries.single()
        assertEquals(ReceiptDeliveryState.FAILED, failed.state)
        assertEquals(TestSms.FAILURE, failed.failureCode)
        assertTrue(model.delivery.canResend, "окончательный отказ не даёт повторить")

        scene.sms.failing = null
        model.resend()

        assertEquals(ReceiptDeliveryState.DELIVERED, model.delivery.deliveries.single().state)
        assertEquals(ReceiptDeliveryState.DELIVERED, scene.kassa.deliveries(sale).single().state, "ядро не доставило")
        assertEquals(DeliveryBench.BUYER, scene.sms.sent.last().destination)
        assertFalse(model.delivery.resendable, "доставленный чек предлагают отправить ещё раз")
        assertNull(model.delivery.problem)
    }

    @Test
    fun `ждущий повтора канал кнопки не даёт, но причину прошлой попытки показывает`() {
        val sale = start().sale()
        scene.kassa.deliverReceipts()
        val model = opened(sale)

        val pending = model.delivery.deliveries.single()
        assertEquals(ReceiptDeliveryState.PENDING, pending.state)
        assertEquals(TestSms.FAILURE, pending.failureCode, "причина прошлой попытки потерялась")
        assertNotNull(pending.nextAttemptAt, "не сказано, когда касса повторит сама")
        assertFalse(model.delivery.resendable, "повтор предложен там, где касса дошлёт сама")
    }

    @Test
    fun `чек без заказанной доставки — объяснение, а не отказ`() {
        val sale = start(ordered = false).sale()
        val model = opened(sale)

        assertTrue(model.delivery.deliveries.isEmpty())
        assertFalse(model.delivery.reading)
        assertFalse(model.delivery.awaitingBfd)
        assertNull(model.delivery.problem)
    }

    @Test
    fun `чек без контакта покупателя — доставки нет, хотя канал заказан`() {
        val sale = start().kassa.sell()
        scene.kassa.deliverReceipts()
        val model = opened(sale)

        assertTrue(scene.kassa.deliveries(sale).isEmpty(), "ядро поставило доставку без контакта")
        assertTrue(scene.sms.sent.isEmpty(), "чек без контакта ушёл по SMS")
        assertTrue(model.delivery.deliveries.isEmpty())
        assertNull(model.delivery.problem)
    }

    @Test
    fun `чек, пробитый без связи, ждёт БФД — доставки ещё нет, и это не пустота`() {
        val offline = start().kassa.offlineSale(buyer = DeliveryBench.BUYER_CONTACT)
        val model = opened(offline)

        assertTrue(model.delivery.awaitingBfd)
        assertTrue(model.delivery.deliveries.isEmpty())

        scene.kassa.resendQueue()
        model.choose(JournalPeriod.of(JournalSpan.Week))
        model.open(offline.documentId)
        assertFalse(model.delivery.awaitingBfd, "после досылки журнал держит прежнее «ждёт БФД»")
    }

    @Test
    fun `отвергнутый БФД чек и отчёт окна доставки не открывают`() {
        val rejected = start().kassa.rejectedSale()
        val model = scene.model().apply { choose(JournalPeriod.of(JournalSpan.Week)) }
        val opening = model.state.value.documents.first { it.docType == "SHIFT_OPEN" }

        model.open(rejected.documentId)
        assertNull(model.state.value.delivery, "отвергнутый чек открыл доставку")
        model.open(opening.id)
        assertNull(model.state.value.delivery, "открытие смены открыло доставку")
    }

    @Test
    fun `отказ кассы остаётся в окне её словами, а не кодом`() {
        val sale = start().sale()
        val model = scene.model().apply { choose(JournalPeriod.of(JournalSpan.Week)) }
        // Пин кассира сменили с другого места: касса его больше не знает.
        scene.signIn.changePin(STALE_PIN)

        model.open(sale.documentId)

        val words = assertNotNull(model.delivery.problem, "отказ кассы не показан в окне")
        assertTrue(words.isNotBlank() && "USER_NOT_FOUND" !in words, "в окне код вместо слов: $words")
        assertFalse(model.delivery.reading, "окно ждёт ответа после отказа")
        assertFalse(model.delivery.resendable)
    }

    @Test
    fun `ушёл кассир — окно чека закрыто`() {
        val sale = start().sale()
        val model = opened(sale)

        scene.signIn.signOut()

        assertNull(model.state.value.delivery)
    }

    private companion object {
        /** Пин, которого касса не знает. */
        const val STALE_PIN = "0000"
    }
}
