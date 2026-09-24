package kz.mybrain.superkassa.presentation.kassa.sale

import kz.kazakhtelecom.proto.v203.PaymentTypeEnum
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.mybrain.superkassa.domain.kassa.model.sale.Adjustment
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.tiyn
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Продажа на настоящем ядре: позиции, количество, скидки и оплата — как
 * их набирает кассир. Итог сверяется с ядром (документ и сумма) и с тем,
 * что получил БФД (строки, скидки, оплаты, сдача).
 */
class SaleCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    /** Пробивает чек и возвращает то, что учёл БФД. */
    private fun SaleViewModel.issued(): TicketRequest {
        issue()
        assertIs<Message.Done>(desk.said, "чек не принят: ${desk.saidText}")
        assertTrue(state.value.basket.positions.isEmpty(), "принятый чек остался в корзине")
        return desk.bfd.countedTickets().last()
    }

    @Test
    fun `наличными со сдачей — в БФД итог, принято и сдача, в ядре документ на ту же сумму`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Хлеб «Тандыр»", "450", quantity = "2")
        model.add("Кумыс", "600")
        model.form.taken("2000")

        val ticket = model.issued()

        assertEquals(LOAVES_AND_KUMYS, ticket.amounts.total.tiyn())
        assertEquals(TWO_THOUSAND, ticket.amounts.taken.tiyn())
        assertEquals(TWO_THOUSAND - LOAVES_AND_KUMYS, ticket.amounts.change.tiyn())
        assertEquals(LOAVES_AND_KUMYS, kassa.sales().single().totalAmount)
    }

    @Test
    fun `весовой товар — дробное количество уходит тысячными и в килограммах`() {
        desk.seated()
        val model = desk.sale()
        model.add("Баранина", "3200", quantity = "1,5", unit = OKEI_KILOGRAM)

        val item = model.issued().items.single().commodity

        assertEquals(ONE_AND_HALF, item?.quantity)
        assertEquals(OKEI_KILOGRAM, item?.measure_unit_code)
        assertEquals(MUTTON, item?.sum.tiyn())
    }

    @Test
    fun `полторы штуки в чек не встают`() {
        desk.seated()
        val model = desk.sale()
        model.entry.editDraft(model.state.value.draft.copy(name = "Хлеб", price = "450", quantity = "1.5"))

        assertFalse(model.entry.addDraft(), "дробное количество у штучного товара принято")
        assertTrue(model.state.value.basket.positions.isEmpty())
    }

    @Test
    fun `скидка на позицию процентом — чек в ядре и в БФД на сумму за вычетом посчитанной скидки`() {
        val kassa = desk.seated()
        val model = desk.sale()
        model.add("Чай", "1000", discount = Adjustment("10", AdjustmentUnit.Percent))
        assertEquals(NINE_HUNDRED, model.state.value.total, "итог на экране не за вычетом скидки")

        val ticket = model.issued()

        assertEquals(NINE_HUNDRED, ticket.amounts.total.tiyn())
        assertEquals(NINE_HUNDRED, kassa.sales().single().totalAmount)
    }

    @Test
    fun `скидка на чек процентом и наценка суммой — взаимоисключающе, в БФД одна из двух`() {
        desk.seated()
        val model = desk.sale()
        model.add("Чай", "1000")
        model.form.discountUnit(AdjustmentUnit.Percent)
        model.form.discount("10")
        model.form.markup("50")

        assertEquals("", model.state.value.form.discount.text, "скидка осталась рядом с наценкой")
        val ticket = model.issued()

        assertNull(ticket.amounts.discount, "скидка ушла в БФД вместе с наценкой")
        assertEquals(FIFTY, ticket.amounts.markup?.sum.tiyn())
        assertEquals(THOUSAND + FIFTY, ticket.amounts.total.tiyn())
    }

    @Test
    fun `скидка на чек вместе со скидкой на позицию — кнопка молчит, в БФД ничего не ушло`() {
        desk.seated()
        val model = desk.sale()
        model.add("Чай", "1000", discount = Adjustment("100"))
        model.form.discount("50")

        model.issue()

        assertEquals(SaleBlock.DiscountScopes, model.state.value.block)
        assertTrue(desk.bfd.countedTickets().isEmpty())
    }

    @Test
    fun `картой и наличными — остаток чека берут наличные, сдача от наличной части`() {
        desk.seated()
        val model = desk.sale()
        model.add("Кумыс", "1500")
        model.payments.addPayment("CARD")
        val card = model.state.value.form.split.entries.indexOfFirst { it.type == "CARD" }
        model.payments.enterPayment(card, "1000")
        model.form.taken("1000")

        val ticket = model.issued()

        val paid = ticket.payments.associate { it.type to it.sum.tiyn() }
        val expected = mapOf(PaymentTypeEnum.PAYMENT_CARD to THOUSAND, PaymentTypeEnum.PAYMENT_CASH to FIVE_HUNDRED)
        assertEquals(expected, paid)
        assertEquals(FIVE_HUNDRED, ticket.amounts.change.tiyn())
    }

    @Test
    fun `картой целиком — сдачи нет и принятого не спрашивают`() {
        desk.seated()
        val model = desk.sale()
        model.add("Кумыс", "1500")
        model.payments.retypePayment(0, "CARD")

        val ticket = model.issued()

        assertEquals(listOf(PaymentTypeEnum.PAYMENT_CARD), ticket.payments.map { it.type })
        assertEquals(0L, ticket.amounts.change.tiyn())
    }

    private companion object {
        const val LOAVES_AND_KUMYS = 150_000L
        const val TWO_THOUSAND = 200_000L
        const val ONE_AND_HALF = 1_500L
        const val MUTTON = 480_000L
        const val NINE_HUNDRED = 90_000L
        const val THOUSAND = 100_000L
        const val FIVE_HUNDRED = 50_000L
        const val FIFTY = 5_000L
    }
}
