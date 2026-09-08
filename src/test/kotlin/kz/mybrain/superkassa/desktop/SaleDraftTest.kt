package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.Amount
import kz.mybrain.superkassa.desktop.ui.sale.DraftField
import kz.mybrain.superkassa.desktop.ui.sale.DraftProblem
import kz.mybrain.superkassa.desktop.ui.sale.PositionDraft
import kz.mybrain.superkassa.desktop.ui.sale.QUANTITY_SCALE
import kz.mybrain.superkassa.desktop.ui.sale.amount
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ввод позиции чека.
 *
 * «Не число» и «мельче допустимого» разведены намеренно: кассир, набравший
 * вес 1,2345 килограмма, должен узнать про предел точности, а не гадать,
 * чем цифра не угодила.
 */
class SaleDraftTest {

    private val filled = PositionDraft(name = "Хлеб", price = "249.90")

    @Test
    fun `заполненная форма даёт позицию`() {
        val position = assertNotNull(filled.position)
        assertEquals("Хлеб", position.name)
        assertEquals(BigDecimal("249.90"), position.price)
        assertEquals(BigDecimal.ONE, position.quantity)
    }

    @Test
    fun `наименование обязательно`() {
        assertEquals(
            DraftProblem.NameMissing,
            filled.copy(name = "  ").problem(DraftField.Name)
        )
    }

    @Test
    fun `цена больше нуля обязательна`() {
        assertEquals(DraftProblem.PriceNotPositive, filled.copy(price = "").problem(DraftField.Price))
        assertEquals(DraftProblem.PriceNotPositive, filled.copy(price = "0").problem(DraftField.Price))
        assertEquals(DraftProblem.PriceNotANumber, filled.copy(price = "сто").problem(DraftField.Price))
    }

    @Test
    fun `цена мельче тиына не принимается`() {
        assertEquals(
            DraftProblem.PriceTooPrecise,
            filled.copy(price = "10.005").problem(DraftField.Price)
        )
    }

    @Test
    fun `весовой товар считается до грамма`() {
        val weighed = filled.copy(quantity = "1.234")
        assertNull(weighed.problem(DraftField.Quantity))
        assertEquals(
            DraftProblem.QuantityTooPrecise,
            filled.copy(quantity = "1.2345").problem(DraftField.Quantity)
        )
    }

    @Test
    fun `нулевое количество позицией не становится`() {
        assertEquals(
            DraftProblem.QuantityNotPositive,
            filled.copy(quantity = "0").problem(DraftField.Quantity)
        )
        assertNull(filled.copy(quantity = "0").position)
    }

    @Test
    fun `скидка не съедает позицию целиком`() {
        assertNull(filled.copy(discount = "100").problem(DraftField.Discount))
        assertEquals(
            DraftProblem.DiscountTooBig,
            filled.copy(discount = "300").problem(DraftField.Discount)
        )
        assertEquals(
            DraftProblem.DiscountTooBig,
            filled.copy(discount = "-1").problem(DraftField.Discount)
        )
    }

    @Test
    fun `сумма из отчёта с неразрывным пробелом разбирается`() {
        assertEquals(BigDecimal("1200.50"), amount("1 200,50").value)
    }

    @Test
    fun `пустое поле отличается от нечисла`() {
        assertTrue(amount("") is Amount.Empty)
        assertTrue(amount("abc") is Amount.NotANumber)
        assertTrue(amount("1.999", QUANTITY_SCALE) is Amount.Value)
        assertTrue(amount("1.999") is Amount.TooPrecise)
    }

    @Test
    fun `после добавления ставка НДС остаётся, а товар забывается`() {
        val used = filled.copy(vatGroup = "VAT_10", discount = "5", storno = true)
        val next = used.cleared()
        assertEquals("VAT_10", next.vatGroup)
        assertEquals("", next.name)
        assertEquals("", next.discount)
        assertEquals(false, next.storno)
    }

    @Test
    fun `сторно доносится до позиции`() {
        val position = assertNotNull(filled.copy(storno = true).position)
        assertTrue(position.total < BigDecimal.ZERO)
    }
}
