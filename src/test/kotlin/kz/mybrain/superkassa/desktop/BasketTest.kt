package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.Basket
import kz.mybrain.superkassa.desktop.ui.sale.Position
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Корзина чека.
 *
 * Итог считается точной десятичной арифметикой: расхождение в тиын между
 * корзиной и чеком у ОФД — это расхождение документов, а не округление.
 */
class BasketTest {

    private fun position(price: String, quantity: String = "1", discount: String = "0") = Position(
        name = "Товар",
        price = BigDecimal(price),
        quantity = BigDecimal(quantity),
        vatGroup = "VAT_16",
        discount = BigDecimal(discount)
    )

    @Test
    fun `итог складывается точно, без потери тиынов`() {
        val basket = Basket()
        basket.add(position("0.10"))
        basket.add(position("0.20"))
        assertEquals(BigDecimal("0.30"), basket.total)
    }

    @Test
    fun `количество умножается на цену`() {
        val basket = Basket()
        basket.add(position("19.99", quantity = "3"))
        assertEquals(BigDecimal("59.97"), basket.total)
    }

    @Test
    fun `скидка вычитается из позиции`() {
        val basket = Basket()
        basket.add(position("100", discount = "15.50"))
        assertEquals(BigDecimal("84.50"), basket.total)
    }

    @Test
    fun `позиция убирается по номеру, остальные остаются`() {
        val basket = Basket()
        basket.add(position("10"))
        basket.add(position("20"))
        basket.removeAt(0)
        assertEquals(1, basket.positions.size)
        assertEquals(BigDecimal("20.00"), basket.total)
    }

    @Test
    fun `удаление несуществующей позиции корзину не портит`() {
        val basket = Basket()
        basket.add(position("10"))
        basket.removeAt(5)
        assertEquals(1, basket.positions.size)
    }

    @Test
    fun `позиции переносятся в чек с теми же ценами`() {
        val basket = Basket()
        basket.add(position("12.34", quantity = "2"))
        val items = basket.toReceiptItems()
        assertEquals(1, items.size)
        assertEquals(BigDecimal("12.34"), items.first().price)
        assertEquals(0, BigDecimal("2").compareTo(items.first().quantity))
        assertEquals("VAT_16", items.first().vatGroup)
    }

    @Test
    fun `нулевая скидка в чек не попадает`() {
        val basket = Basket()
        basket.add(position("10"))
        assertTrue(basket.toReceiptItems().first().discountSum == null)
    }
}
