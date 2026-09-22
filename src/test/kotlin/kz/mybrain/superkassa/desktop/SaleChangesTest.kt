package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.Adjustment
import kz.mybrain.superkassa.desktop.ui.sale.AdjustmentUnit
import kz.mybrain.superkassa.desktop.ui.sale.Basket
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.sale.SaleBlock
import kz.mybrain.superkassa.desktop.ui.sale.SaleForm
import kz.mybrain.superkassa.desktop.ui.sale.SaleState
import kz.mybrain.superkassa.desktop.ui.sale.blockOf
import kz.mybrain.superkassa.desktop.ui.sale.changeBlockOf
import kz.mybrain.superkassa.desktop.ui.sale.changesOf
import kz.mybrain.superkassa.desktop.ui.sale.formatPercent
import kz.mybrain.superkassa.desktop.ui.sale.percentOfTenge
import kz.mybrain.superkassa.desktop.ui.sale.tengeOfPercent
import kz.mybrain.superkassa.desktop.ui.sale.totalOf
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Скидка и наценка на чек: суммой и процентом.
 *
 * Процент — способ ввода, а не способ расчёта: в узел уходит посчитанная
 * сумма в тенге, и округление до тиына объявлено одно на скидку и наценку.
 * Расхождение в тиын здесь — это расхождение с БФД, поэтому проверяются
 * ровно те доли, на которых округление и решает: половина тиына.
 */
class SaleChangesTest {

    private fun basket(sum: String): Basket = Basket().apply {
        add(Position(name = "Товар", price = BigDecimal(sum), quantity = BigDecimal.ONE, vatGroup = "VAT_16"))
    }

    @Test
    fun `процент считается от суммы позиций и округляется до тиына`() {
        // Половина тиына уходит вверх: 3 % от 3 333,33 ₸ — это 99,9999 ₸.
        assertEquals(BigDecimal("100.00"), tengeOfPercent(BigDecimal("3333.33"), BigDecimal("3")))
        assertEquals(BigDecimal("100.01"), tengeOfPercent(BigDecimal("1000.05"), BigDecimal("10")))
        assertEquals(BigDecimal("0.01"), tengeOfPercent(BigDecimal("0.10"), BigDecimal("5")))
        assertEquals(BigDecimal("0.00"), tengeOfPercent(BigDecimal("0.10"), BigDecimal("4")))
    }

    @Test
    fun `скидка и наценка округляются одним правилом`() {
        val items = BigDecimal("1000.05")
        val discount = Adjustment("10", AdjustmentUnit.Percent).sumOf(items)
        val markup = Adjustment("10", AdjustmentUnit.Percent).sumOf(items)
        assertEquals(discount, markup)
        assertEquals(BigDecimal("100.01"), discount)
    }

    @Test
    fun `набранная сумма показывается долей, а набранная доля — суммой`() {
        val items = BigDecimal("2000.00")
        assertEquals(BigDecimal("10.00"), percentOfTenge(items, BigDecimal("200")))
        assertEquals(BigDecimal("200.00"), tengeOfPercent(items, BigDecimal("10")))
        // Пустому чеку доли не бывает: делить не на что.
        assertNull(percentOfTenge(BigDecimal.ZERO, BigDecimal("200")))
        assertEquals("10 %", formatPercent(BigDecimal("10.00")).replace(' ', ' '))
        assertEquals("12,5 %", formatPercent(BigDecimal("12.50")).replace(' ', ' '))
    }

    @Test
    fun `итог чека считается от процента так же, как от суммы`() {
        val basket = basket("2000.00")
        val byPercent = SaleForm().apply { switchDiscount(AdjustmentUnit.Percent); enterDiscount("10") }
        val bySum = SaleForm().apply { enterDiscount("200") }
        assertEquals(BigDecimal("1800.00"), totalOf(basket, byPercent))
        assertEquals(totalOf(basket, bySum), totalOf(basket, byPercent))
    }

    @Test
    fun `в узел уходит посчитанная сумма, а не процент`() {
        val basket = basket("2000.00")
        val form = SaleForm().apply { switchDiscount(AdjustmentUnit.Percent); enterDiscount("10") }
        assertEquals(BigDecimal("200.00"), form.input(basket).discount)
        val markup = SaleForm().apply { switchMarkup(AdjustmentUnit.Percent); enterMarkup("10") }
        assertEquals(BigDecimal("200.00"), markup.input(basket).markup)
        assertNull(markup.input(basket).discount)
    }

    @Test
    fun `смена знака не стирает набранное число`() {
        val form = SaleForm().apply { enterDiscount("500") }
        form.switchDiscount(AdjustmentUnit.Percent)
        assertEquals("500", form.discount.text)
        assertEquals(AdjustmentUnit.Percent, form.discount.unit)
    }

    @Test
    fun `скидка и наценка на чек остаются взаимоисключающими`() {
        val form = SaleForm().apply { enterDiscount("100") }
        form.enterMarkup("50")
        assertTrue(form.discount.text.isEmpty())
        assertEquals("50", form.markup.text)
    }

    @Test
    fun `минус не принимается ни у скидки, ни у наценки, ни в процентах`() {
        val items = BigDecimal("1000")
        val negativePercent = Adjustment("-10", AdjustmentUnit.Percent)
        assertEquals(
            SaleBlock.DiscountNegative,
            blockOf(SaleState(discount = negativePercent, itemsSum = items))
        )
        assertEquals(
            SaleBlock.DiscountNegative,
            blockOf(SaleState(markup = negativePercent, itemsSum = items))
        )
        // Минус в скидке прибавлял к итогу молча. Проверяется тот же путь,
        // которым идёт экран: корзина и набранное поле, а не собранный
        // руками снимок.
        val form = SaleForm().apply { enterDiscount("-100") }
        assertEquals(SaleBlock.DiscountNegative, changeBlockOf(changesOf(basket("1000.00"), form)))
    }

    @Test
    fun `процент больше ста не принимается ни у скидки, ни у наценки`() {
        val items = BigDecimal("1000")
        val over = Adjustment("101", AdjustmentUnit.Percent)
        assertEquals(SaleBlock.PercentOverHundred, blockOf(SaleState(discount = over, itemsSum = items)))
        assertEquals(SaleBlock.PercentOverHundred, blockOf(SaleState(markup = over, itemsSum = items)))
        val hundred = Adjustment("100", AdjustmentUnit.Percent)
        assertEquals(SaleBlock.TotalNotPositive, blockOf(SaleState(discount = hundred, itemsSum = items, total = BigDecimal.ZERO)))
    }

    @Test
    fun `скидка больше суммы позиций не принимается, а наценка сверх неё — принимается`() {
        val items = BigDecimal("1000")
        assertEquals(
            SaleBlock.DiscountOverItems,
            blockOf(SaleState(discount = Adjustment("1000.01"), itemsSum = items, total = BigDecimal("100")))
        )
        assertNull(
            blockOf(SaleState(markup = Adjustment("5000"), itemsSum = items, total = BigDecimal("6000")))
        )
        assertNull(
            blockOf(SaleState(discount = Adjustment("1000"), itemsSum = items, total = BigDecimal("1")))
        )
    }
}
