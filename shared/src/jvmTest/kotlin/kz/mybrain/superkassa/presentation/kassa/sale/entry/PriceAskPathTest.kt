package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScene
import kz.mybrain.superkassa.presentation.kassa.sale.SaleViewModel
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalVatRates
import kz.mybrain.superkassa.presentation.kassa.sale.position.measureUnits
import kz.mybrain.superkassa.presentation.kassa.sale.saleModel
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Путь кассира от штрихкода до строки чека.
 *
 * На площадке позиция национального каталога встала в чек с нулевой ценой
 * молча: каталог цен не несёт вовсе. Здесь тот же путь проходится
 * нажатиями — код в поле, Enter, окно, цена, Enter, — и проверяется,
 * что без окна нулевая позиция в чек не попадает, а позиция с ценой
 * по-прежнему добавляется сразу.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PriceAskPathTest {

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(found: NomenclatureLookupResponse): SaleViewModel {
        val core = SaleScene.core().apply { on("lookupNomenclature") { found } }
        return saleModel(CoreScene.app(core, SaleScene.signedIn()))
    }

    @Test
    fun `позиция каталога без цены попадает в чек только через окно`() {
        val model = model(SaleScene.found(price = "0").let { it.copy(item = it.item?.copy(name = "Сыр на развес")) })

        RenderProbe(width = TILL, height = TILL) { Field(model) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(FIELD_X, FIELD_Y))
            probe.type("2000")
            probe.key(Key.Enter)
            repeat(SETTLE) { probe.frame() }
            assertTrue(model.state.value.basket.positions.isEmpty(), "нулевая позиция встала в чек без вопроса")
            probe.type("450")
            probe.key(Key.Enter)
            repeat(SETTLE) { probe.frame() }
        }

        val added = model.state.value.basket.positions.single()
        assertEquals(0, tenge("450").compareTo(added.price))
        assertEquals("Сыр на развес", added.name)
    }

    @Test
    fun `позиция каталога с ценой окна не открывает`() {
        val model = model(SaleScene.found(price = "249.90"))

        RenderProbe(width = TILL, height = TILL) { Field(model) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(FIELD_X, FIELD_Y))
            probe.type("2001")
            probe.key(Key.Enter)
            repeat(SETTLE) { probe.frame() }
        }

        val added = model.state.value.basket.positions.single()
        assertEquals(tenge("249.90"), added.price)
        assertEquals(decimal("1"), added.quantity)
    }

    @Composable
    private fun Field(model: SaleViewModel) {
        val state by model.state.collectAsState()
        CompositionLocalProvider(
            LocalSaleTexts provides textsOf(Language.Ru).kassa.sale,
            LocalVatRates provides state.vat(Language.Ru, LocalStrings.current.enums),
            LocalUnits provides measureUnits(Language.Ru)
        ) {
            Box(Modifier.padding(Spacing.fieldGap)) { BarcodeField(state, model.entry) }
        }
    }

    private companion object {
        const val SETTLE = 20

        const val TILL = 640

        const val FIELD_X = 240f
        const val FIELD_Y = 40f
    }
}
