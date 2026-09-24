package kz.mybrain.superkassa.presentation.kassa.sale.position

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.component.BasketCard
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.kassa.saleTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.shot
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Окно подробностей строки чека.
 *
 * Снимки — `/tmp/position-details-*.png`, и смотрит их человек: влезли
 * ли строки с марками и кодами, читается ли скупая позиция без пустых
 * подписей. Открытие с карточки проверяется нажатием: значки на строке
 * своё нажатие оставляют себе, а остальная карточка ведёт в окно.
 */
class PositionDetailsShots {

    private val texts = saleTexts(Language.Ru)
    private val units = listOf(
        MeasureUnit("796", "шт"),
        MeasureUnit("116", "кг")
    )
    private val rates = listOf(VatRate("NO_VAT", "Без НДС"), VatRate("VAT_16", "НДС", percent = 16))

    private val full = Position(
        name = "Коньяк «Казахстан» 0,5 л",
        nameKk = "«Қазақстан» коньягы 0,5 л",
        price = tenge("12500"),
        quantity = decimal("2"),
        vatGroup = "VAT_16",
        discount = tenge("500"),
        measureUnitCode = "796",
        ntin = "KZ01234567890123",
        barcode = "4870001234567",
        exciseStamps = listOf("KZ0000000001", "KZ0000000002")
    )

    private val bare = Position(
        name = "Пакет",
        price = tenge("15"),
        quantity = decimal("1"),
        vatGroup = "NO_VAT"
    )

    @Test
    fun `окно рисует полную и скупую позицию по-разному`() {
        val fullFrame = shot("position-details-full", full)
        val bareFrame = shot("position-details-bare", bare)
        val storno = shot("position-details-storno", full.copy(storno = true))

        val frames = listOf(fullFrame, bareFrame, storno)
        assertEquals(frames.size, frames.map { it.toList() }.distinct().size, "состояния окна неотличимы")
    }

    /** Нажатие на наименование в строке корзины открывает окно; значок удаления — нет. */
    @Test
    fun `карточка строки открывает подробности, а значок делает своё`() {
        val basket = Basket().add(full)
        var removed = false
        RenderProbe(width = TILL, height = TILL) { Sheet(basket) { removed = true } }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.click(Offset(NAME_X, ROW_Y))
            assertTrue(probe.changedFrom(before), "нажатие на строку окна не открыло")
            File("/tmp/position-details-opened.png").writeBytes(probe.frame())
        }
        assertFalse(removed, "нажатие на строку удалило её")
    }

    private fun shot(name: String, position: Position): ByteArray {
        val frame = RenderProbe { Dialog(position, onDismiss = {}) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        File("/tmp/$name.png").writeBytes(frame)
        assertTrue(frame.isNotEmpty(), "снимок $name пуст")
        return frame
    }

    @Composable
    private fun Dialog(position: Position, onDismiss: () -> Unit) {
        PositionDetailsDialog(
            details = position.details(),
            rates = rates,
            onDismiss = onDismiss,
            onStorno = {},
            onRemove = {}
        )
    }

    /** Лист чека с теми же надписями и справочниками, что на экране продажи. */
    @Composable
    private fun Sheet(basket: Basket, onRemove: (Int) -> Unit) {
        CompositionLocalProvider(
            LocalSaleTexts provides texts,
            LocalVatRates provides rates,
            LocalUnits provides units
        ) {
            Box(Modifier.fillMaxSize().padding(Spacing.screen)) {
                BasketCard(basket, Modifier.fillMaxSize(), onStorno = {}, onExcise = {}, onRemove = onRemove)
            }
        }
    }

    private companion object {
        const val SETTLE = 20

        /** Лист чека в узком окне: строка одна и стоит под шапкой листа. */
        const val TILL = 640

        /** Наименование первой строки: слева, под шапкой с числом позиций. */
        const val NAME_X = 120f
        const val ROW_Y = 110f
    }
}
