package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Правила, по которым кнопка «Пробить чек» доступна или нет.
 *
 * Коды отказов, от которых защищают эти правила, сняты с работающего узла:
 * SHIFT_NOT_OPEN, PAYMENT_TYPE_NOT_SUPPORTED,
 * RECEIPT_DISCOUNT_SCOPES_CONFLICT и «taken must be >= sum of CASH payments».
 */
class SaleRulesTest {

    @Test
    fun `полностью заполненный чек ничем не заблокирован`() {
        assertNull(blockOf(SaleState(total = tenge("100"))))
    }

    @Test
    fun `сначала называется самое общее — касса, потом пин`() {
        assertEquals(SaleBlock.NoKkm, blockOf(SaleState(hasKkm = false, hasPin = false)))
        assertEquals(SaleBlock.NoPin, blockOf(SaleState(hasPin = false)))
    }

    @Test
    fun `закрытая смена и блокировка кассы названы раньше состава чека`() {
        assertEquals(SaleBlock.KkmBlocked, blockOf(SaleState(kkmBlocked = true, positions = 0)))
        assertEquals(SaleBlock.ShiftClosed, blockOf(SaleState(shiftOpen = false, positions = 0)))
    }

    @Test
    fun `пустая корзина не даёт пробить чек`() {
        assertEquals(SaleBlock.EmptyBasket, blockOf(SaleState(positions = 0)))
    }

    @Test
    fun `оплата в кредит и тарой отвергается до отправки`() {
        assertEquals(SaleBlock.PaymentUnsupported, blockOf(SaleState(paymentCodes = listOf("CREDIT"))))
        assertEquals(SaleBlock.PaymentUnsupported, blockOf(SaleState(paymentCodes = listOf("TARE"))))
        assertNull(blockOf(SaleState(paymentCodes = listOf("CARD"))))
    }

    @Test
    fun `скидка и наценка на чек со знаком минус чек пробить не дают`() {
        // Минус в скидке прибавлял к итогу, минус в наценке — вычитал:
        // «Итого» расходилось с набранным, и кнопка при этом была нажимаема.
        assertEquals(
            SaleBlock.DiscountNegative,
            blockOf(SaleState(discount = Adjustment("-100"), itemsSum = tenge("100")))
        )
        assertEquals(
            SaleBlock.DiscountNegative,
            blockOf(SaleState(markup = Adjustment("-100"), itemsSum = tenge("100")))
        )
        assertNull(
            blockOf(
                SaleState(
                    discount = Adjustment("10"),
                    itemsSum = tenge("100"),
                    total = tenge("90")
                )
            )
        )
    }

    @Test
    fun `скидка на позицию и скидка на чек вместе запрещены`() {
        val both = SaleState(
            hasItemDiscount = true,
            discount = Adjustment("5"),
            itemsSum = tenge("100")
        )
        assertEquals(SaleBlock.DiscountScopes, blockOf(both))
        assertNull(blockOf(both.copy(discount = Adjustment())))
        assertNull(blockOf(both.copy(hasItemDiscount = false)))
    }

    @Test
    fun `нулевой итог чеком не становится`() {
        assertEquals(SaleBlock.TotalNotPositive, blockOf(SaleState(total = 0L)))
    }

    @Test
    fun `принято меньше итога — только для наличных`() {
        val short = SaleState(total = tenge("100"), taken = tenge("50"))
        assertEquals(SaleBlock.TakenTooSmall, blockOf(short))
        assertNull(blockOf(short.copy(paymentCodes = listOf("CARD"), cashSum = 0L)))
        assertNull(blockOf(short.copy(taken = null)))
    }

    @Test
    fun `сдача считается только когда принятого хватает`() {
        assertEquals(tenge("400"), changeOf(tenge("1000"), tenge("600")))
        assertEquals(0L, changeOf(tenge("600"), tenge("600")))
        assertNull(changeOf(tenge("500"), tenge("600")))
        assertNull(changeOf(null, tenge("600")))
    }

    @Test
    fun `ИИН и БИН — ровно двенадцать цифр или ничего`() {
        assertTrue(binAccepted(""))
        assertTrue(binAccepted("123456789012"))
        assertFalse(binAccepted("12345678901"))
        assertFalse(binAccepted("1234567890123"))
        assertEquals(SaleBlock.CustomerBin, blockOf(SaleState(customerBin = "123")))
    }

    @Test
    fun `набранный с ошибкой контакт покупателя не даёт пробить чек, а пустой — даёт`() {
        assertEquals(SaleBlock.CustomerContact, blockOf(SaleState(contactMalformed = true)))
        val typed = SaleForm().chooseContactKind(ContactKind.Email).enterContact("buyer@example")
        assertTrue(typed.contact.malformed)
        assertNull(blockOf(SaleState(contactMalformed = SaleForm().contact.malformed)))
    }

    @Test
    fun `после принятого чека контакт забывается, а его вид остаётся`() {
        val typed = SaleForm().chooseContactKind(ContactKind.Telegram).enterContact("123456789")
        val next = typed.next()
        assertEquals(ContactKind.Telegram, next.contact.kind)
        assertTrue(next.contact.empty, "контакт прошлого покупателя перешёл в следующий чек")
    }
}
