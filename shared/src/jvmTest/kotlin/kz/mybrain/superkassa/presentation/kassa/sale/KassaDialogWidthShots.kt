package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.SettingsMeasure
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.presentation.kassa.sale.component.ExciseDialog
import kz.mybrain.superkassa.presentation.kassa.sale.position.PositionDetailsDialog
import kz.mybrain.superkassa.presentation.kassa.sale.position.VatRate
import kz.mybrain.superkassa.presentation.kassa.sale.position.details
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Окна «Позиция чека» и «Акцизные марки» на телефоне и на широком окне.
 *
 * Окна стояли фиксированной ширины формы, и на телефоне 360–412 точек
 * правый край окна с кнопками уходил за экран. На компактном окне окно
 * встаёт по ширине экрана с полями, на широком — прежней ширины формы.
 * Прежде окно шириной формы упиралось в оба края экрана телефона.
 * Кадры — `/tmp/kassa-dialog-<окно>-<ширина>x<высота>.png`.
 */
class KassaDialogWidthShots {

    private val rates = listOf(VatRate("NO_VAT", "Без НДС"), VatRate("VAT_16", "НДС", percent = 16))

    private val position = Position(
        name = "Коньяк «Казахстан» выдержанный пятилетний 0,5 л",
        nameKk = "«Қазақстан» бес жылдық коньягы 0,5 л",
        price = tenge("12500"),
        quantity = decimal("2"),
        vatGroup = "VAT_16",
        measureUnitCode = "796",
        barcode = "4870001234567",
        exciseStamps = listOf("KZ0000000001", "KZ0000000002")
    )

    @Test
    fun `позиция чека не выходит за экран телефона`() = everywhere("position") {
        PositionDetailsDialog(position.details(), rates, onDismiss = {}, onStorno = {}, onRemove = {})
    }

    @Test
    fun `акцизные марки не выходят за экран телефона`() = everywhere("excise") {
        CompositionLocalProvider(LocalSaleTexts provides textsOf(Language.Ru).kassa.sale) {
            ExciseDialog(position.exciseStamps, onChanged = {}, onDismiss = {})
        }
    }

    private fun everywhere(kind: String, content: @Composable () -> Unit) =
        SIZES.forEach { (width, height) -> shoot(kind, width, height, content) }

    private fun shoot(kind: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val name = "kassa-dialog-$kind-${width}x$height"
        RenderProbe(width, height) {
            Surface(Modifier.fillMaxSize()) { }
            content()
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/$name.png").writeBytes(probe.frame())
            val nodes = probe.semantics()
            val dialog = SettingsMeasure.surfaces(nodes, from = 0, minWidth = 1)
                .filter { it.width < width || it.height < height }
                .maxByOrNull { it.width * it.height }
            val controls = SettingsMeasure.controls(nodes)
            println("$name: окно ${dialog?.left}..${dialog?.right}; кнопки ${controls.map { it.left..it.right }}")
            assertTrue(dialog != null, "$name: окна нет")
            assertTrue(controls.all { it.left >= 0 && it.right <= width }, "$name: кнопка за краем экрана")
            fits(name, dialog, width)
        }
    }

    /** На широком окне — прежняя ширина формы, на телефоне — поля по бокам. */
    private fun fits(name: String, dialog: SettingsMeasure.Box, width: Int) {
        if (width >= WIDE) {
            val wide = dialog.width + 2 * INSET
            assertEquals(Sizes.formDialog.value.toInt(), wide, "$name: на широком окне ширина сменилась")
        } else {
            val margin = Spacing.cardGap.value.toInt()
            val left = dialog.left - INSET
            val right = width - dialog.right - INSET
            assertTrue(left >= margin && right >= margin, "$name: окно без полей")
        }
    }

    private companion object {
        const val SETTLE = 20
        const val WIDE = 1280

        /**
         * Отступ содержимого окна Material 3 от его края: мерится поверхность
         * содержимого, а край окна — на этот отступ дальше.
         */
        const val INSET = 24

        /** Телефоны 360 и 412 точек и окно кассы на ноутбуке. */
        val SIZES = listOf(360 to 800, 412 to 915, WIDE to 800)
    }
}
