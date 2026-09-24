package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Акцизные марки в чеке.
 *
 * Марка на бутылке и пачке — учётный документ КГД: узел принимает её
 * полем `listExciseStamp`, и потеря марки по дороге означает продажу
 * подакцизного товара без прослеживания.
 *
 * Повтор проверяется отдельно: одну и ту же бутылку подносят к сканеру
 * дважды, и две одинаковые марки в чеке — расхождение с КГД, а не описка.
 */
class SaleExciseTest {

    private fun bottle(stamps: List<String> = emptyList()) = Position(
        name = "Вода питьевая 0,5 л",
        price = tenge("450.00"),
        quantity = decimal("1"),
        vatGroup = "VAT_16",
        ntin = "0200091550792",
        exciseStamps = stamps
    )

    @Test
    fun `марки и НТИН доходят до позиции чека`() {
        var basket = Basket()
        basket = basket.add(bottle(listOf("KZ0000000001", "KZ0000000002")))

        val item = basket.toItems().single()

        assertEquals(listOf("KZ0000000001", "KZ0000000002"), item.listExciseStamp)
        assertEquals("0200091550792", item.ntin)
    }

    @Test
    fun `позиция без марок не шлёт пустой перечень`() {
        var basket = Basket()
        basket = basket.add(bottle())

        assertNull(basket.toItems().single().listExciseStamp)
    }

    @Test
    fun `марка добавляется в позицию, уже стоящую в чеке`() {
        var basket = Basket()
        basket = basket.add(bottle())

        basket = basket.stampAt(0, ExciseRules.accept(basket.positions[0].exciseStamps, "KZ0000000001"))

        assertEquals(listOf("KZ0000000001"), basket.positions[0].exciseStamps)
    }

    @Test
    fun `повторная марка перечень не удваивает`() {
        val stamps = ExciseRules.accept(listOf("KZ0000000001"), " KZ0000000001 ")

        assertEquals(listOf("KZ0000000001"), stamps)
        assertEquals(ExciseRefusal.Repeated, ExciseRules.refusal(listOf("KZ0000000001"), "KZ0000000001"))
    }

    @Test
    fun `слипшийся поток сканера маркой не считается`() {
        val tooLong = "K".repeat(101)

        assertEquals(emptyList(), ExciseRules.accept(emptyList(), tooLong))
        assertEquals(ExciseRefusal.TooLong, ExciseRules.refusal(emptyList(), tooLong))
        assertNull(ExciseRules.refusal(emptyList(), "KZ0000000001"))
    }
}
