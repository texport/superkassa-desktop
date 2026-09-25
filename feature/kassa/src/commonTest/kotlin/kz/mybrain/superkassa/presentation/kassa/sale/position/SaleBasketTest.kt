package kz.mybrain.superkassa.presentation.kassa.sale.position

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleState
import kz.mybrain.superkassa.domain.kassa.model.sale.blockOf
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Корзина чека: сторно, скидки и показ сумм.
 *
 * Расхождение в тиын между корзиной и чеком у ОФД — расхождение документов,
 * а не округление на экране, поэтому проверяется точное значение.
 */
class SaleBasketTest {

    private fun position(
        price: String,
        quantity: String = "1",
        discount: String = "0",
        storno: Boolean = false
    ) = Position(
        name = "Товар",
        price = tenge(price),
        quantity = decimal(quantity),
        vatGroup = "VAT_16",
        discount = tenge(discount),
        storno = storno
    )

    /**
     * Пока чек не пробит, сторно — черновая отметка: кассир нажал не ту
     * строку и вправе снять её тем же значком. Прежде снять было нечем,
     * и строку приходилось удалять и набирать заново.
     */
    @Test
    fun `сторно снимается повторным нажатием, пока чек не пробит`() {
        var basket = Basket()
        basket = basket.add(position("100"))

        basket = basket.stornoAt(0)
        assertTrue(basket.positions.single().storno, "сторно поставлено")

        basket = basket.stornoAt(0)
        assertFalse(basket.positions.single().storno, "сторно снято")
    }

    @Test
    fun `сторно уводит позицию в минус, а не просто помечает её`() {
        var basket = Basket()
        basket = basket.add(position("100"))
        basket = basket.add(position("30", storno = true))
        assertEquals(tenge("70.00"), basket.total)
    }

    @Test
    fun `дробное количество умножается точно`() {
        var basket = Basket()
        basket = basket.add(position("249.90", quantity = "0.375"))
        assertEquals(tenge("93.71"), basket.total)
    }

    @Test
    fun `скидка на позицию видна корзине целиком`() {
        var basket = Basket()
        basket = basket.add(position("100"))
        assertFalse(basket.hasItemDiscount)
        basket = basket.add(position("100", discount = "10"))
        assertTrue(basket.hasItemDiscount)
    }

    /**
     * Скидка, забравшая строку целиком, оставляет в чеке ноль.
     *
     * Соседние позиции держат итог чека положительным, и общая проверка
     * итога такую строку пропускала: товар за ноль уходил в фискальный
     * чек, а экран об этом молчал.
     */
    @Test
    fun `строка, съеденная скидкой до нуля, чек пробить не даёт`() {
        var basket = Basket()
        basket = basket.add(position("30", discount = "30"))
        basket = basket.add(position("250"))

        assertEquals(tenge("0.00"), basket.positions.first().lineSum)
        assertTrue(basket.hasZeroLine, "нулевая строка видна корзине")
        assertEquals(
            SaleBlock.ZeroLine,
            blockOf(SaleState(positions = 2, hasZeroLine = true, total = tenge("250")))
        )
    }

    @Test
    fun `скидка на чек вычитается из итога, наценка прибавляется`() {
        var basket = Basket()
        basket = basket.add(position("100"))
        assertEquals(tenge("90.00"), basket.totalWith(tenge("10"), null))
        assertEquals(tenge("110.00"), basket.totalWith(null, tenge("10")))
    }

    @Test
    fun `итог ниже нуля не опускается`() {
        var basket = Basket()
        basket = basket.add(position("10"))
        assertEquals(tenge("0.00"), basket.totalWith(tenge("999"), null))
    }

    @Test
    fun `минус у суммы меньше тенге не теряется`() {
        assertTrue(Money.formatTiyn(tenge("-0.50")).startsWith("−"))
        assertFalse(Money.formatTiyn(tenge("0.50")).startsWith("−"))
    }

    /**
     * Сведения строки разделяет общий знак набора.
     *
     * Состав строки собирается вне Compose и разбирается проверкой,
     * а не глазом на снимке. Знаки берутся из общего набора: набранные
     * на месте, они неотличимы от общих на экране и расходятся с ними
     * на первой же правке — так лист чека однажды уже разошёлся
     * с соседними списками приложения.
     */
    @Test
    fun `состав строки набран общими знаками`() {
        val line = positionLine(
            position("3450.00", quantity = "1.450", discount = "50.00").copy(exciseStamps = listOf("AB1")),
            unit = "кг",
            vat = "НДС 16%",
            texts = textsOf(Language.Ru).kassa.sale
        )

        assertTrue(line.contains(Glyphs.TIMES), "количество умножается общим знаком: $line")
        assertEquals(4, line.split(Glyphs.SEPARATOR).size, "сведения разделены общим знаком: $line")
        assertTrue(line.startsWith("1,450 кг"), "дробь отделена общим знаком: $line")
    }

    @Test
    fun `неизвестная ставка показывается кодом, а не пустотой`() {
        assertEquals("НДС 16%", vatTitle(NODE_RATES, "VAT_16"))
        assertEquals("VAT_99", vatTitle(NODE_RATES, "VAT_99"))
    }

    @Test
    fun `сторно уходит в чек признаком, обычная позиция — без него`() {
        var basket = Basket()
        basket = basket.add(position("10", storno = true))
        basket = basket.add(position("10"))
        assertEquals(true, basket.toItems()[0].isStorno)
        assertEquals(null, basket.toItems()[1].isStorno)
    }

    private companion object {

        /** Ставки в том виде, в каком их отдаёт `/dictionaries/vat-groups`. */
        val NODE_RATES = listOf(
            VatRate("NO_VAT", "Без НДС"),
            VatRate("VAT_0", "НДС 0%"),
            VatRate("VAT_5", "НДС 5%"),
            VatRate("VAT_10", "НДС 10%"),
            VatRate("VAT_12", "НДС 12%"),
            VatRate("VAT_16", "НДС 16%")
        )
    }
}
