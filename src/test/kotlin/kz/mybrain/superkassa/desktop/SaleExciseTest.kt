package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.sale.Basket
import kz.mybrain.superkassa.desktop.ui.sale.ExciseRules
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    private val texts = saleTexts(Language.Ru)

    private fun bottle(stamps: List<String> = emptyList()) = Position(
        name = "Вода питьевая 0,5 л",
        price = BigDecimal("450.00"),
        quantity = BigDecimal.ONE,
        vatGroup = "VAT_16",
        ntin = "0200091550792",
        exciseStamps = stamps
    )

    @Test
    fun `марки и НТИН доходят до позиции чека`() {
        val basket = Basket()
        basket.add(bottle(listOf("KZ0000000001", "KZ0000000002")))

        val item = basket.toReceiptItems().single()

        assertEquals(listOf("KZ0000000001", "KZ0000000002"), item.listExciseStamp)
        assertEquals("0200091550792", item.ntin)
    }

    @Test
    fun `позиция без марок не шлёт пустой перечень`() {
        val basket = Basket()
        basket.add(bottle())

        assertNull(basket.toReceiptItems().single().listExciseStamp)
    }

    @Test
    fun `марка добавляется в позицию, уже стоящую в чеке`() {
        val basket = Basket()
        basket.add(bottle())

        basket.stampAt(0, ExciseRules.accept(basket.positions[0].exciseStamps, "KZ0000000001"))

        assertEquals(listOf("KZ0000000001"), basket.positions[0].exciseStamps)
    }

    @Test
    fun `повторная марка перечень не удваивает`() {
        val stamps = ExciseRules.accept(listOf("KZ0000000001"), " KZ0000000001 ")

        assertEquals(listOf("KZ0000000001"), stamps)
        assertNotNull(ExciseRules.refusal(listOf("KZ0000000001"), "KZ0000000001", texts))
    }

    @Test
    fun `слипшийся поток сканера маркой не считается`() {
        val tooLong = "K".repeat(101)

        assertEquals(emptyList(), ExciseRules.accept(emptyList(), tooLong))
        assertEquals(texts.exciseTooLong, ExciseRules.refusal(emptyList(), tooLong, texts))
        assertNull(ExciseRules.refusal(emptyList(), "KZ0000000001", texts))
    }
}
