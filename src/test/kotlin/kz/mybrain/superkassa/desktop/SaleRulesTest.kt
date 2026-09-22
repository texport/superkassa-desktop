package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.DomainField
import kz.mybrain.superkassa.desktop.ui.sale.SaleBlock
import kz.mybrain.superkassa.desktop.ui.sale.SaleState
import kz.mybrain.superkassa.desktop.ui.sale.binAccepted
import kz.mybrain.superkassa.desktop.ui.sale.blockOf
import kz.mybrain.superkassa.desktop.ui.sale.changeOf
import kz.mybrain.superkassa.desktop.ui.strings.RussianStrings
import kz.mybrain.superkassa.desktop.ui.strings.paymentTextsRu
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsRu
import java.math.BigDecimal
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
        assertNull(blockOf(SaleState(total = BigDecimal("100"))))
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
            blockOf(SaleState(receiptDiscount = BigDecimal("-100")))
        )
        assertEquals(
            SaleBlock.DiscountNegative,
            blockOf(SaleState(receiptMarkup = BigDecimal("-100")))
        )
        assertNull(blockOf(SaleState(receiptDiscount = BigDecimal("10"), total = BigDecimal("90"))))
    }

    @Test
    fun `скидка на позицию и скидка на чек вместе запрещены`() {
        val both = SaleState(hasItemDiscount = true, receiptDiscount = BigDecimal("5"))
        assertEquals(SaleBlock.DiscountScopes, blockOf(both))
        assertNull(blockOf(both.copy(receiptDiscount = null)))
        assertNull(blockOf(both.copy(hasItemDiscount = false)))
    }

    @Test
    fun `нулевой итог чеком не становится`() {
        assertEquals(SaleBlock.TotalNotPositive, blockOf(SaleState(total = BigDecimal.ZERO)))
    }

    @Test
    fun `незаполненное отраслевое поле названо поимённо`() {
        val block = blockOf(SaleState(missingDomainField = DomainField.CarNumber))
        assertEquals(SaleBlock.DomainFields, block)
        val words = block?.reason(RussianStrings.sale, saleTextsRu, paymentTextsRu, DomainField.CarNumber).orEmpty()
        assertTrue(words.contains(RussianStrings.sale.carNumber))
    }

    @Test
    fun `принято меньше итога — только для наличных`() {
        val short = SaleState(total = BigDecimal("100"), taken = BigDecimal("50"))
        assertEquals(SaleBlock.TakenTooSmall, blockOf(short))
        assertNull(blockOf(short.copy(paymentCodes = listOf("CARD"), cashSum = BigDecimal.ZERO)))
        assertNull(blockOf(short.copy(taken = null)))
    }

    @Test
    fun `сдача считается только когда принятого хватает`() {
        assertEquals(BigDecimal("400"), changeOf(BigDecimal("1000"), BigDecimal("600")))
        assertEquals(BigDecimal.ZERO, changeOf(BigDecimal("600"), BigDecimal("600")))
        assertNull(changeOf(BigDecimal("500"), BigDecimal("600")))
        assertNull(changeOf(null, BigDecimal("600")))
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
    fun `каждая причина названа словами на всех трёх языках`() {
        SaleBlock.entries.forEach { block ->
            val words = block.reason(RussianStrings.sale, saleTextsRu, paymentTextsRu)
            assertTrue(words.isNotBlank(), "причина ${block.name} без текста")
        }
    }
}
