package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.Percent
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Скидка и наценка на чек: суммой и процентом.
 *
 * Процент — способ ввода, а не способ расчёта: в кассу уходит посчитанная
 * сумма в тенге, и округление до тиына объявлено одно на скидку и наценку.
 * Расхождение в тиын здесь — это расхождение с БФД, поэтому проверяются
 * ровно те доли, на которых округление и решает: половина тиына.
 */
class SaleChangesTest {

    private fun basket(sum: String): Basket =
        Basket().add(Position(name = "Товар", price = tenge(sum), quantity = decimal("1"), vatGroup = "VAT_16"))

    @Test
    fun `процент считается от суммы позиций и округляется до тиына`() {
        // Половина тиына уходит вверх: 3 % от 3 333,33 ₸ — это 99,9999 ₸.
        assertEquals(tenge("100.00"), Percent.of(tenge("3333.33"), decimal("3")))
        assertEquals(tenge("100.01"), Percent.of(tenge("1000.05"), decimal("10")))
        assertEquals(tenge("0.01"), Percent.of(tenge("0.10"), decimal("5")))
        assertEquals(tenge("0.00"), Percent.of(tenge("0.10"), decimal("4")))
    }

    @Test
    fun `скидка и наценка округляются одним правилом`() {
        val items = tenge("1000.05")
        val discount = Adjustment("10", AdjustmentUnit.Percent).sumOf(items)
        val markup = Adjustment("10", AdjustmentUnit.Percent).sumOf(items)
        assertEquals(discount, markup)
        assertEquals(tenge("100.01"), discount)
    }

    @Test
    fun `набранная сумма показывается долей, а набранная доля — суммой`() {
        val items = tenge("2000.00")
        assertEquals(decimal("10.00"), Percent.share(items, tenge("200")))
        assertEquals(tenge("200.00"), Percent.of(items, decimal("10")))
        // Пустому чеку доли не бывает: делить не на что.
        assertNull(Percent.share(0L, tenge("200")))
    }

    @Test
    fun `итог чека считается от процента так же, как от суммы`() {
        val basket = basket("2000.00")
        val byPercent = SaleForm().switchDiscount(AdjustmentUnit.Percent).enterDiscount("10")
        val bySum = SaleForm().enterDiscount("200")
        assertEquals(tenge("1800.00"), totalOf(basket, byPercent))
        assertEquals(totalOf(basket, bySum), totalOf(basket, byPercent))
    }

    @Test
    fun `в кассу уходит посчитанная сумма, а не процент`() {
        val basket = basket("2000.00")
        val form = SaleForm().switchDiscount(AdjustmentUnit.Percent).enterDiscount("10")
        val discounted = SaleReceipt(basket, form).command("kkm-1", "1234")
        assertEquals(tenge("200.00"), discounted.discountSum?.let(Tenge::of))
        assertNull(discounted.discountPercent)
        val markup = SaleForm().switchMarkup(AdjustmentUnit.Percent).enterMarkup("10")
        val marked = SaleReceipt(basket, markup).command("kkm-1", "1234")
        assertEquals(tenge("200.00"), marked.markupSum?.let(Tenge::of))
        assertNull(marked.discountSum)
    }

    @Test
    fun `смена знака не стирает набранное число`() {
        val form = SaleForm().enterDiscount("500").switchDiscount(AdjustmentUnit.Percent)
        assertEquals("500", form.discount.text)
        assertEquals(AdjustmentUnit.Percent, form.discount.unit)
    }

    @Test
    fun `скидка и наценка на чек остаются взаимоисключающими`() {
        val form = SaleForm().enterDiscount("100").enterMarkup("50")
        assertTrue(form.discount.text.isEmpty())
        assertEquals("50", form.markup.text)
    }

    @Test
    fun `минус не принимается ни у скидки, ни у наценки, ни в процентах`() {
        val items = tenge("1000")
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
        val form = SaleForm().enterDiscount("-100")
        assertEquals(SaleBlock.DiscountNegative, changeBlockOf(changesOf(basket("1000.00"), form)))
    }

    @Test
    fun `процент больше ста не принимается ни у скидки, ни у наценки`() {
        val items = tenge("1000")
        val over = Adjustment("101", AdjustmentUnit.Percent)
        assertEquals(SaleBlock.PercentOverHundred, blockOf(SaleState(discount = over, itemsSum = items)))
        assertEquals(SaleBlock.PercentOverHundred, blockOf(SaleState(markup = over, itemsSum = items)))
        val hundred = Adjustment("100", AdjustmentUnit.Percent)
        assertEquals(
            SaleBlock.TotalNotPositive,
            blockOf(SaleState(discount = hundred, itemsSum = items, total = 0L))
        )
    }

    /**
     * Чек из одних сторно: сумма позиций ниже нуля.
     *
     * Ненабранная скидка считалась нулём, а ноль «больше» отрицательной
     * суммы позиций — и касса краснила оба пустых поля и требовала
     * уменьшить скидку, которой кассир не набирал. Настоящая помеха
     * у такого чека одна: итог не положителен.
     */
    @Test
    fun `чек из одних сторно не винит в этом скидку`() {
        val basket = Basket()
            .add(Position(name = "Товар", price = tenge("100"), quantity = decimal("1"), vatGroup = "VAT_16"))
            .stornoAt(0)
        val state = changesOf(basket, SaleForm())

        assertNull(changeBlockOf(state), "пустые поля скидки и наценки помехой не являются")
        assertFalse(state.discountWrong(), "поле скидки не краснеет")
        assertFalse(state.markupWrong(), "поле наценки не краснеет")
        assertEquals(
            SaleBlock.TotalNotPositive,
            blockOf(state.copy(positions = 1, total = 0L))
        )
    }

    @Test
    fun `скидка больше суммы позиций не принимается, а наценка сверх неё — принимается`() {
        val items = tenge("1000")
        assertEquals(
            SaleBlock.DiscountOverItems,
            blockOf(SaleState(discount = Adjustment("1000.01"), itemsSum = items, total = tenge("100")))
        )
        assertNull(
            blockOf(SaleState(markup = Adjustment("5000"), itemsSum = items, total = tenge("6000")))
        )
        assertNull(
            blockOf(SaleState(discount = Adjustment("1000"), itemsSum = items, total = tenge("1")))
        )
    }

    /**
     * Скидка, набранная не числом, не исчезает молча.
     *
     * «10,005» и «abc» прежде считались пустым полем: поле не краснело,
     * кнопка горела, и чек уходил по полной цене.
     */
    @Test
    fun `скидка, набранная не числом, названа причиной и красит своё поле`() {
        listOf("10,005", "abc", "10..5").forEach { typed ->
            val state = changesOf(basket("1000.00"), SaleForm().enterDiscount(typed))
            assertEquals(SaleBlock.ChangeNotANumber, changeBlockOf(state), "скидка «$typed»")
            assertTrue(state.discountWrong(), "поле скидки краснеет: «$typed»")
            assertFalse(state.markupWrong(), "поле наценки не краснеет: «$typed»")
        }
        val markup = changesOf(basket("1000.00"), SaleForm().enterMarkup("5%"))
        assertEquals(SaleBlock.ChangeNotANumber, changeBlockOf(markup))
        assertTrue(markup.markupWrong())
    }
}
