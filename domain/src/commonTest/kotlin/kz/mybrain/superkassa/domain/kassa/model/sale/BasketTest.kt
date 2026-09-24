package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.tenge
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
        price = tenge(price),
        quantity = decimal(quantity),
        vatGroup = "VAT_16",
        discount = tenge(discount)
    )

    @Test
    fun `итог складывается точно без потери тиынов`() {
        var basket = Basket()
        basket = basket.add(position("0.10"))
        basket = basket.add(position("0.20"))
        assertEquals(tenge("0.30"), basket.total)
    }

    @Test
    fun `количество умножается на цену`() {
        var basket = Basket()
        basket = basket.add(position("19.99", quantity = "3"))
        assertEquals(tenge("59.97"), basket.total)
    }

    @Test
    fun `скидка вычитается из позиции`() {
        var basket = Basket()
        basket = basket.add(position("100", discount = "15.50"))
        assertEquals(tenge("84.50"), basket.total)
    }

    @Test
    fun `позиция убирается по номеру остальные остаются`() {
        var basket = Basket()
        basket = basket.add(position("10"))
        basket = basket.add(position("20"))
        basket = basket.removeAt(0)
        assertEquals(1, basket.positions.size)
        assertEquals(tenge("20.00"), basket.total)
    }

    @Test
    fun `удаление несуществующей позиции корзину не портит`() {
        var basket = Basket()
        basket = basket.add(position("10"))
        basket = basket.removeAt(5)
        assertEquals(1, basket.positions.size)
    }

    @Test
    fun `позиции переносятся в чек с теми же ценами`() {
        var basket = Basket()
        basket = basket.add(position("12.34", quantity = "2"))
        val items = basket.toItems()
        assertEquals(1, items.size)
        assertEquals(tenge("12.34"), items.first().price.let(Tenge::of))
        assertEquals(tenge("2"), items.first().quantity.let(Tenge::of))
        assertEquals("VAT_16", items.first().vatGroup)
    }

    @Test
    fun `нулевая скидка в чек не попадает`() {
        var basket = Basket()
        basket = basket.add(position("10"))
        assertTrue(basket.toItems().first().discountSum == null)
    }
}
