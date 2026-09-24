package kz.mybrain.superkassa.presentation.kassa.sale.entry

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.model.entry.LookupProblem
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScene
import kz.mybrain.superkassa.presentation.kassa.sale.SaleViewModel
import kz.mybrain.superkassa.presentation.kassa.sale.saleModel
import kz.mybrain.superkassa.presentation.words.kassa.lookupProblemWords
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Поиск по штрихкоду в справочнике кассы.
 *
 * Ненайденный товар, неотвеченный справочник и заблокированная касса —
 * три разные беды, и одна фраза «нет такого штрихкода» на все три
 * отправляла кассира искать несуществующую беду с товаром.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SaleLookupTest {
    private val notices = Notices()
    private val sale = textsOf(Language.Ru).common.sale

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun search(core: FakeCore, blockReason: Int? = null): SaleViewModel {
        val kkm = CoreScene.kkm(state = if (blockReason == null) "ACTIVE" else "BLOCKED", blockReasonCode = blockReason)
        val model = saleModel(CoreScene.app(core, SaleScene.signedIn(kkm), notices))
        model.entry.typeBarcode(SaleScene.BARCODE)
        assertTrue(model.entry.search(), "набранный код ищется")
        return model
    }

    private fun words(model: SaleViewModel): String {
        val state = model.state.value
        return lookupProblemWords(state.search.problem!!, state.kkm?.blockReasonCode, Language.Ru, sale)
    }

    @Test
    fun `справочник ответил и товара в нём нет — это отсутствие`() {
        val model = search(SaleScene.core().apply { on("lookupNomenclature") { SaleScene.missing() } })

        assertEquals(LookupProblem.Missing, model.state.value.search.problem)
        assertEquals(sale.barcodeMissing, words(model))
        assertNull(notices.last, "отсутствие товара — не беда кассы")
    }

    @Test
    fun `молчащий справочник не выдаётся за отсутствие товара`() {
        val model = search(SaleScene.core().apply { on("lookupNomenclature") { SaleScene.missing(resultCode = -1) } })

        assertEquals(LookupProblem.Unavailable, model.state.value.search.problem)
        assertEquals(sale.barcodeUnavailable, words(model))
        assertNotEquals(sale.barcodeMissing, words(model))
    }

    @Test
    fun `касса не смогла спросить — тоже недоступный справочник, и об этом сказано`() {
        val model = search(SaleScene.core().apply { on("lookupNomenclature") { error("no link") } })

        assertEquals(LookupProblem.Unavailable, model.state.value.search.problem)
        assertIs<Message.Failed>(notices.last)
    }

    @Test
    fun `заблокированная касса названа блокировкой и её причиной`() {
        val core = SaleScene.core().apply { refuse("lookupNomenclature", "KKM_BLOCKED", ru = "Касса заблокирована") }
        val model = search(core, blockReason = INVALID_TOKEN)

        assertEquals(LookupProblem.Blocked, model.state.value.search.problem)
        assertEquals(textsOf(Language.Ru).kassa.blockReason.invalidToken, words(model))
    }

    @Test
    fun `отсутствие самой кассы не выдаётся за отсутствие товара`() {
        val core = SaleScene.core().apply { refuse("lookupNomenclature", "KKM_NOT_FOUND", ru = "Касса не найдена") }
        val model = search(core)

        assertEquals(LookupProblem.Unavailable, model.state.value.search.problem)
        assertEquals(Message.Refusal("Касса не найдена", "KKM_NOT_FOUND"), notices.last)
    }

    @Test
    fun `найденный товар с ценой встаёт в чек, и поле очищается`() {
        val model = search(SaleScene.core().apply { on("lookupNomenclature") { SaleScene.found() } })

        val state = model.state.value
        val position = state.basket.positions.single()
        assertEquals(0, tenge("450.00").compareTo(position.price))
        assertEquals("0200091550792", position.ntin)
        assertEquals("796", position.measureUnitCode, "без единицы из справочника — штука")
        assertEquals("", state.search.barcode)
        assertNull(state.search.problem)
    }

    @Test
    fun `товар без цены в чек не встаёт, а цену спрашивают`() {
        val model = search(SaleScene.core().apply { on("lookupNomenclature") { SaleScene.found(price = "0") } })

        val state = model.state.value
        assertTrue(state.basket.positions.isEmpty(), "нулевая строка встала в чек молча")
        assertEquals("Вода питьевая 0,5 л", state.search.asking?.name)
    }

    private companion object {
        const val INVALID_TOKEN = 1002
    }
}
