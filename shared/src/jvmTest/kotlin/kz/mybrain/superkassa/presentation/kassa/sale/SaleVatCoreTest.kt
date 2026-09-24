package kz.mybrain.superkassa.presentation.kassa.sale

import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import io.github.texport.superkassa.testing.api.kassa.VatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * НДС на весь чек и по позициям — на настоящем ядре и тестовом БФД.
 *
 * Продажа идёт через модель экрана, как у кассира: ни в одном способе
 * касса не отвечает `RECEIPT_VAT_SCOPES_CONFLICT`, и до БФД налог доходит
 * ровно на том уровне, который выбран. У неплательщика выбора нет вовсе.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SaleVatCoreTest {
    private val directory: File = createTempDirectory("kassa-vat-").toFile()
    private val bench = appBench(directory)
    private val notices = Notices()
    private val texts = textsOf(Language.Ru).kassa.sale

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        Dispatchers.resetMain()
        bench.close()
        directory.deleteRecursively()
    }

    private fun kassa(vat: VatMode): ReadyKassa =
        bench.registerKassa(appKassa(adminPin = "7391", cashierPin = "4826", vat = vat)).also { it.openShift() }

    private fun model(kassa: ReadyKassa): SaleViewModel {
        val cashier = bench.api.authenticate(kassa.kkmId, kassa.cashierPin)
        val signIn = SignIn().apply { enter(kassa.info(), cashier, kassa.cashierPin) }
        val app = CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, notices)
        return saleModel(app.services, app.areas.kassa).also { it.visit() }
    }

    /** Позиция руками; ставка — та, с которой начинается новая позиция, если не задана. */
    private fun SaleViewModel.add(name: String, price: String, vat: String? = null) {
        val draft = state.value.draft.copy(name = name, price = price, measureUnitCode = "796")
        entry.editDraft(vat?.let { draft.copy(vatGroup = it) } ?: draft)
        assertTrue(entry.addDraft(), "позиция «$name» не встала в чек")
    }

    private fun SaleViewModel.issued() {
        issue()
        assertIs<Message.Done>(notices.last, "чек не принят: ${notices.last}")
        assertTrue(state.value.basket.positions.isEmpty(), "принятый чек остался в корзине")
    }

    @Test
    fun `по позициям — у каждой позиции своя ставка, новая начинается со ставки кассы`() {
        val kassa = kassa(VatMode.Payer(VatGroup.VAT_16))
        val model = model(kassa)
        model.add("Хлеб «Тандыр»", "450")
        model.add("Кумыс", "900", vat = "VAT_5")

        model.issued()

        val ticket = bench.bfd.countedTickets().last()
        assertTrue(ticket.taxes.isEmpty(), "НДС по позициям ушёл налогом чека: ${ticket.taxes}")
        assertEquals(listOf(listOf(PERCENT_16), listOf(PERCENT_5)), ticket.items.map { it.percents() })
    }

    @Test
    fun `на весь чек — одна ставка чека, у позиций ставок нет`() {
        val kassa = kassa(VatMode.Payer(VatGroup.VAT_16))
        val model = model(kassa)
        model.add("Хлеб «Тандыр»", "450")
        model.add("Кумыс", "900", vat = "VAT_5")
        model.form.vat.scope(VatScope.Receipt)
        assertEquals("VAT_16", model.state.value.receiptVat, "ставка чека по умолчанию — не ставка кассы")
        model.form.vat.rate("VAT_10")

        model.issued()

        val ticket = bench.bfd.countedTickets().last()
        assertEquals(listOf(PERCENT_10), ticket.taxes.map { it.percent })
        assertTrue(ticket.items.all { it.percents().isEmpty() }, "у позиций остались ставки при НДС на весь чек")
    }

    @Test
    fun `неплательщик пробивает чек без выбора способа и без ставки чека`() {
        val kassa = kassa(VatMode.NotPayer)
        val model = model(kassa)
        model.add("Хлеб «Тандыр»", "450")
        model.form.vat.scope(VatScope.Receipt)

        model.issued()

        assertFalse(model.state.value.vatPayer)
        assertTrue(bench.bfd.countedTickets().last().taxes.isEmpty())
    }

    @Test
    fun `переключатель способа виден плательщику и не виден неплательщику`() {
        assertTrue(texts.vatOnReceipt in shown(kassa(VatMode.Payer(VatGroup.VAT_16))), "плательщику не дали выбрать")
        assertFalse(texts.vatOnReceipt in shown(kassa(VatMode.NotPayer)), "неплательщику предложен НДС на весь чек")
    }

    /** Надписи экрана продажи кассы [kassa]. */
    private fun shown(kassa: ReadyKassa): List<String?> {
        val state = model(kassa).state.value
        return RenderProbe(width = WIDE, height = TALL) { SaleContent(state) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.nodes().map { it.text }
        }
    }

    private companion object {
        /** Ставки в CPCR — тысячными долями процента. */
        const val PERCENT_16 = 16_000
        const val PERCENT_10 = 10_000
        const val PERCENT_5 = 5_000

        const val WIDE = 1400
        const val TALL = 2400
        const val SETTLE = 10
    }
}

/** Ставки налогов позиции чека, как их получил БФД. */
private fun TicketRequest.Item.percents(): List<Int> =
    commodity?.taxes.orEmpty().map { it.percent }
