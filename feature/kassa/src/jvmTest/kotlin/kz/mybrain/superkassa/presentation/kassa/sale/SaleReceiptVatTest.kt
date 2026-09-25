package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.NO_VAT
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Что уходит в чек при НДС на весь чек и при НДС по позициям.
 *
 * Экран только показывает выбор; запрос собирается из него здесь, и в каждом
 * способе ставка стоит на одном уровне — у чека или у позиций.
 */
class SaleReceiptVatTest {
    private val enums = textsOf(Language.Ru).common.enums
    private val payer = CoreScene.kkm().copy(taxRegime = "VAT_PAYER", defaultVatGroup = "VAT_16")
    private val notPayer = CoreScene.kkm().copy(taxRegime = "NO_VAT", defaultVatGroup = NO_VAT)

    private fun sale(kkm: KkmResponse, form: SaleForm) =
        SaleUiState(kkm = kkm, signedIn = true, shiftOpen = true, basket = BASKET, form = form)

    @Test
    fun `по позициям — у каждой позиции своя ставка, у чека ставки нет`() {
        val command = sale(payer, SaleForm()).receipt.command("kkm-1", "1234")

        assertNull(command.vatGroup)
        assertEquals(listOf("VAT_16", "VAT_5"), command.items.map { it.vatGroup })
    }

    @Test
    fun `на весь чек — ставка кассы, у позиций ставок нет`() {
        val state = sale(payer, SaleForm().switchVat(VatScope.Receipt))

        val command = state.receipt.command("kkm-1", "1234")

        assertEquals("VAT_16", command.vatGroup)
        assertEquals(listOf(null, null), command.items.map { it.vatGroup })
        assertTrue(state.positionVat(Language.Ru, enums).isEmpty(), "ставки позиций видны при НДС на весь чек")
    }

    @Test
    fun `на весь чек — выбранная кассиром ставка`() {
        val form = SaleForm().switchVat(VatScope.Receipt).chooseVat("VAT_5")

        assertEquals("VAT_5", sale(payer, form).receipt.command("kkm-1", "1234").vatGroup)
    }

    @Test
    fun `возврат к позициям возвращает их ставки в чек`() {
        val form = SaleForm().switchVat(VatScope.Receipt).chooseVat("VAT_5").switchVat(VatScope.Positions)
        val state = sale(payer, form)

        val command = state.receipt.command("kkm-1", "1234")

        assertNull(command.vatGroup)
        assertEquals(listOf("VAT_16", "VAT_5"), command.items.map { it.vatGroup })
        assertTrue(state.positionVat(Language.Ru, enums).size > 1, "ставки позиций скрыты при НДС по позициям")
    }

    @Test
    fun `у неплательщика выбора нет, и ставки чека в запросе не бывает`() {
        val state = sale(notPayer, SaleForm().switchVat(VatScope.Receipt).chooseVat("VAT_16"))

        val command = state.receipt.command("kkm-1", "1234")

        assertFalse(state.vatPayer)
        assertNull(command.vatGroup)
        assertNull(state.receiptVat)
    }

    @Test
    fun `следующий чек начинается тем же способом НДС`() {
        val form = SaleForm().switchVat(VatScope.Receipt).chooseVat("VAT_5")

        assertEquals(form.vat, form.next().vat)
    }

    private companion object {
        val BASKET = Basket(
            listOf(
                Position("Хлеб «Тандыр»", tenge("450.00"), decimal("1"), "VAT_16"),
                Position("Кумыс", tenge("900.00"), decimal("1"), "VAT_5")
            )
        )
    }
}
