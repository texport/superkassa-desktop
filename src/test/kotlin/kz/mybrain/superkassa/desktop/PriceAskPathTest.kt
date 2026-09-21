package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import kotlinx.serialization.json.JsonPrimitive
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.NomenclatureItem
import kz.mybrain.superkassa.desktop.ui.sale.BarcodeField
import kz.mybrain.superkassa.desktop.ui.sale.LocalSaleTexts
import kz.mybrain.superkassa.desktop.ui.sale.LocalUnits
import kz.mybrain.superkassa.desktop.ui.sale.LocalVatRates
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.sale.vatRatesOf
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal
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
class PriceAskPathTest {

    @Test
    fun `позиция каталога без цены попадает в чек только через окно`() {
        val added = mutableListOf<Position>()
        val session = KassaScene.session(
            "price-ask-none",
            shift = KassaScene.openShift(),
            catalogue = NomenclatureItem(barcode = "2000", name = "Сыр на развес", measureUnitCode = "166")
        )

        RenderProbe(width = TILL, height = TILL) { Field(session, added) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(FIELD_X, FIELD_Y))
            probe.type("2000")
            probe.key(Key.Enter)
            repeat(SETTLE) { probe.frame() }
            assertTrue(added.isEmpty(), "нулевая позиция встала в чек без вопроса")
            probe.type("450")
            probe.key(Key.Enter)
            repeat(SETTLE) { probe.frame() }
        }

        assertEquals(0, BigDecimal("450").compareTo(added.single().price))
        assertEquals("Сыр на развес", added.single().name)
    }

    /** Позиция с ценой встаёт в чек сразу: прежний быстрый путь не тронут. */
    @Test
    fun `позиция каталога с ценой окна не открывает`() {
        val added = mutableListOf<Position>()
        val session = KassaScene.session(
            "price-ask-priced",
            shift = KassaScene.openShift(),
            catalogue = NomenclatureItem(
                barcode = "2001",
                name = "Хлеб «Тары»",
                price = JsonPrimitive("249.90"),
                measureUnitCode = "796"
            )
        )

        RenderProbe(width = TILL, height = TILL) { Field(session, added) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(FIELD_X, FIELD_Y))
            probe.type("2001")
            probe.key(Key.Enter)
            repeat(SETTLE) { probe.frame() }
        }

        assertEquals(0, BigDecimal("249.90").compareTo(added.single().price))
        assertEquals(0, BigDecimal.ONE.compareTo(added.single().quantity))
    }

    /** Поле штрихкода в кассовой колонке, с теми же надписями и ставками. */
    @Composable
    private fun Field(session: Session, added: MutableList<Position>) {
        CompositionLocalProvider(
            LocalSaleTexts provides saleTexts(Language.Ru),
            LocalVatRates provides vatRatesOf(session, LocalStrings.current.enums),
            LocalUnits provides session.units
        ) {
            Box(Modifier.padding(Spacing.screen)) { BarcodeField(session) { added.add(it) } }
        }
    }

    private companion object {
        /** Сколько кадров даётся поиску и окну, чтобы доехать до экрана. */
        const val SETTLE = 20

        /** Ширина кассовой колонки: столько места у поля штрихкода в окне кассира. */
        const val TILL = 640

        /** Середина поля штрихкода: по нему кассир и ставит курсор. */
        const val FIELD_X = 240f
        const val FIELD_Y = 40f
    }
}
