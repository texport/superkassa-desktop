package kz.mybrain.superkassa.domain.kassa.model

import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * НДС на весь чек или по позициям — взаимоисключающе, как скидка и наценка.
 *
 * В каждом способе в запрос уходит ставка только одного уровня: чек со
 * ставкой и у чека, и у позиции касса отвергает (`RECEIPT_VAT_SCOPES_CONFLICT`).
 */
class ReceiptVatTest {

    @Test
    fun `по позициям — ставка у позиции, ставки чека нет`() {
        val vat = ReceiptVat()

        assertEquals(VatScope.Positions, vat.scopeAt(payer = true))
        assertNull(vat.receiptRate(payer = true, kassaRate = "VAT_16"))
        assertEquals("VAT_5", vat.positionRate(payer = true, own = "VAT_5"))
    }

    @Test
    fun `на весь чек — ставка кассы, пока кассир не выбрал другую, а у позиций ставок нет`() {
        val vat = ReceiptVat().switchTo(VatScope.Receipt)

        assertEquals("VAT_16", vat.receiptRate(payer = true, kassaRate = "VAT_16"))
        assertEquals("VAT_5", vat.choose("VAT_5").receiptRate(payer = true, kassaRate = "VAT_16"))
        assertNull(vat.positionRate(payer = true, own = "VAT_16"))
    }

    @Test
    fun `переключение туда и обратно не теряет выбранной ставки чека`() {
        val vat = ReceiptVat().switchTo(VatScope.Receipt).choose("VAT_10")

        val back = vat.switchTo(VatScope.Positions).switchTo(VatScope.Receipt)

        assertEquals("VAT_10", back.receiptRate(payer = true, kassaRate = "VAT_16"))
    }

    /** Неплательщик налог не выделяет: ставки чека у него нет, даже если способ выбран прежде. */
    @Test
    fun `у неплательщика НДС всегда по позициям`() {
        val vat = ReceiptVat(VatScope.Receipt, "VAT_16")

        assertEquals(VatScope.Positions, vat.scopeAt(payer = false))
        assertNull(vat.receiptRate(payer = false, kassaRate = "NO_VAT"))
        assertEquals("NO_VAT", vat.positionRate(payer = false, own = "NO_VAT"))
    }

    @Test
    fun `ни в одном способе ставка не уходит сразу у чека и у позиции`() {
        for (payer in listOf(true, false)) {
            for (scope in VatScope.entries) {
                val vat = ReceiptVat(scope)
                val both = vat.receiptRate(payer, "VAT_16") != null && vat.positionRate(payer, "VAT_16") != null
                assertFalse(both, "ставка у чека и у позиции: payer=$payer, $scope")
            }
        }
    }

    @Test
    fun `плательщик — касса с режимом, отличным от «без НДС»`() {
        assertFalse(paysVat(CoreScene.kkm().copy(taxRegime = NO_VAT_REGIME)))
        assertTrue(paysVat(CoreScene.kkm().copy(taxRegime = "VAT_PAYER")))
        assertTrue(paysVat(CoreScene.kkm().copy(taxRegime = null)))
    }
}
