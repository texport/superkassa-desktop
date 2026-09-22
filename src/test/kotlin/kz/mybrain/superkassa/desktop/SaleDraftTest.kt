package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.Adjustment
import kz.mybrain.superkassa.desktop.ui.sale.AdjustmentUnit
import kz.mybrain.superkassa.desktop.ui.sale.Amount
import kz.mybrain.superkassa.desktop.ui.sale.DraftField
import kz.mybrain.superkassa.desktop.ui.sale.DraftProblem
import kz.mybrain.superkassa.desktop.ui.sale.PositionDraft
import kz.mybrain.superkassa.desktop.ui.sale.QUANTITY_SCALE
import kz.mybrain.superkassa.desktop.ui.sale.amount
import kz.mybrain.superkassa.desktop.ui.sale.tengeOfPercent
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
        assertNull(filled.copy(discount = Adjustment("100")).problem(DraftField.Discount))
        assertEquals(
            DraftProblem.DiscountTooBig,
            filled.copy(discount = Adjustment("300")).problem(DraftField.Discount)
        )
        // Скидка ровно в стоимость позиции — та же беда, что и большая:
        // строка выходит нулевой, а товар за ноль вставал в чек молча.
        assertEquals(
            DraftProblem.DiscountTooBig,
            filled.copy(discount = Adjustment("249.90")).problem(DraftField.Discount)
        )
        assertNull(filled.copy(discount = Adjustment("249.90")).position)
        // Отрицательная скидка названа своей причиной: она меньше
        // стоимости позиции, и «скидка не может быть больше стоимости»
        // кассиру, набравшему «-1», ничего не объясняло.
        assertEquals(
            DraftProblem.DiscountNegative,
            filled.copy(discount = Adjustment("-1")).problem(DraftField.Discount)
        )
    }

    @Test
    fun `доля и сумма дают одну и ту же скидку позиции`() {
        // Строка на 1 500 тенге: десятая её часть — 150, и оба способа
        // ввода обязаны дать кассиру одно число.
        val line = filled.copy(price = "500.00", quantity = "3")
        val byTenge = line.copy(discount = Adjustment("150.00"))
        val byPercent = line.copy(discount = Adjustment("10", AdjustmentUnit.Percent))

        assertEquals(BigDecimal("150.00"), byTenge.discountSum)
        assertEquals(byTenge.discountSum, byPercent.discountSum)
        assertEquals(byTenge.position?.discount, byPercent.position?.discount)
    }

    @Test
    fun `доля скидки позиции округляется как скидка на чек`() {
        // Треть от 100,01 не делится на тиыны нацело: округление обязано
        // совпасть с тем, которым считается скидка на весь чек, — иначе
        // строка расходится с чеком на тиын.
        val line = filled.copy(price = "100.01", quantity = "1")
        val third = line.copy(discount = Adjustment("33.33", AdjustmentUnit.Percent))

        assertEquals(tengeOfPercent(BigDecimal("100.01"), BigDecimal("33.33")), third.discountSum)
    }

    @Test
    fun `доля скидки позиции держится в пределах ноль сто`() {
        val line = filled.copy(price = "500.00", quantity = "2")
        val over = line.copy(discount = Adjustment("101", AdjustmentUnit.Percent))
        val whole = line.copy(discount = Adjustment("100", AdjustmentUnit.Percent))
        val below = line.copy(discount = Adjustment("-5", AdjustmentUnit.Percent))

        assertEquals(DraftProblem.DiscountOverPercent, over.problem(DraftField.Discount))
        assertNull(over.position)
        // Сто процентов отдают строку даром: нулевая строка — не продажа,
        // и в чек она уходить не должна ни долей, ни суммой.
        assertEquals(DraftProblem.DiscountTooBig, whole.problem(DraftField.Discount))
        assertNull(whole.position)
        assertEquals(DraftProblem.DiscountNegative, below.problem(DraftField.Discount))
        // Девяносто девять процентов строку оставляют: она ещё продажа.
        assertNull(line.copy(discount = Adjustment("99", AdjustmentUnit.Percent)).problem(DraftField.Discount))
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
        val used = filled.copy(vatGroup = "VAT_10", discount = Adjustment("5"), storno = true)
        val next = used.cleared()
        assertEquals("VAT_10", next.vatGroup)
        assertEquals("", next.name)
        assertEquals("", next.discount.text)
        assertEquals(false, next.storno)
    }

    @Test
    fun `сторно доносится до позиции`() {
        val position = assertNotNull(filled.copy(storno = true).position)
        assertTrue(position.total < BigDecimal.ZERO)
    }
}
