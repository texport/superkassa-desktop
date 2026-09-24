package kz.mybrain.superkassa.presentation.kassa.sale.position

import kz.mybrain.superkassa.domain.kassa.model.paysVat
import kz.mybrain.superkassa.domain.kassa.model.sale.NO_VAT
import kz.mybrain.superkassa.domain.kassa.model.sale.defaultVatOf
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ставки НДС, показанные кассиру, обязаны дойти до ОФД.
 *
 * Касса в режиме «Без НДС» давала выбрать ставку в позиции чека, писала
 * «НДС 12 %» в строке корзины и печатала её, а в ОФД уходила позиция без
 * налога: в пакете узла рядом лежали "vatGroup":"VAT_12",
 * "taxRegime":"NO_VAT" и "ticketTaxes":[]. Выбора, который никуда
 * не уходит, быть не должно.
 */
class SaleVatRegimeTest {

    private val enums = stringsOf(Language.Ru).enums

    private fun kkmWith(regime: String?, group: String?) = CoreScene.kkm().copy(
        kkmId = "4166498c-d0c1-406e-863d-20458dfd3040",
        kkmKgdId = "260940000021",
        taxRegime = regime,
        defaultVatGroup = group
    )

    @Test
    fun `неплательщику НДС ставок не предлагают`() {
        val kkm = kkmWith("NO_VAT", NO_VAT)

        val rates = vatRatesOf(emptyList(), kkm, Language.Ru, enums)

        assertFalse(paysVat(kkm))
        assertEquals(listOf(NO_VAT), rates.map { it.code })
    }

    @Test
    fun `плательщику НДС перечень ставок остаётся`() {
        val kkm = kkmWith("VAT_PAYER", "VAT_16")

        val rates = vatRatesOf(emptyList(), kkm, Language.Ru, enums)

        assertTrue(paysVat(kkm))
        assertTrue(rates.size > 1, rates.map { it.code }.toString())
        assertTrue(rates.any { it.code == "VAT_16" })
    }

    /** Ставка новой позиции берётся из перечня, а он уже сужен режимом. */
    @Test
    fun `новая позиция неплательщика начинается без НДС`() {
        val kkm = kkmWith("NO_VAT", "VAT_12")

        assertEquals(NO_VAT, defaultVatOf(kkm, emptyList()))
    }

    /** Режим не назван — ставки остаются: прятать их у плательщика нельзя. */
    @Test
    fun `касса без названного режима ставок не теряет`() {
        val kkm = kkmWith(null, null)

        assertTrue(paysVat(kkm))
        assertTrue(vatRatesOf(emptyList(), kkm, Language.Ru, enums).size > 1)
    }
}
