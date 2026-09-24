package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.presentation.common.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.theme.size.ContentWidths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ряд, который переносится, и предел ширины содержимого.
 *
 * Ряд плашек журнала в узком окне ставил последнюю плашку столбиком
 * по букве. Проверяется ширина каждой плашки: в переносящемся ряду она
 * та же, что в просторном, а ряд растёт вниз. Для сравнения рядом —
 * обычный `Row` в той же ширине: его последняя плашка сжата.
 *
 * Кадры остаются в `/tmp/adaptive-wrap-*.png`.
 */
class AdaptiveRowsTest {

    private fun chipWidths(width: Int, wrap: Boolean): Pair<List<Int>, Int> {
        val widths = IntArray(FILTERS.size)
        var height = 0
        val chips: @Composable () -> Unit = {
            FILTERS.forEachIndexed { index, label ->
                AssistChip(
                    onClick = {},
                    label = { Text(label) },
                    modifier = Modifier.onGloballyPositioned { widths[index] = it.size.width }
                )
            }
        }
        val frame = RenderProbe(width = width, height = HEIGHT) {
            Box(modifier = Modifier.onGloballyPositioned { height = it.size.height }) {
                if (wrap) WrapRow { chips() } else Row { chips() }
            }
        }.use { probe -> probe.frame().also { probe.frame() } }
        File("/tmp/adaptive-wrap-${if (wrap) "flow" else "row"}-$width.png").writeBytes(frame)
        return widths.toList() to height
    }

    @Test
    fun `ряд переносит плашку целиком, а не сжимает её до буквы`() {
        val (roomy, oneRow) = chipWidths(WIDE, wrap = true)
        val (narrow, rows) = chipWidths(NARROW, wrap = true)
        val (squeezed, _) = chipWidths(NARROW, wrap = false)
        println("плашки: просторно $roomy, перенос $narrow, обычный ряд $squeezed; высота $oneRow → $rows")
        assertEquals(roomy, narrow, "в узком ряду плашки не той же ширины, что в просторном")
        assertTrue(rows > oneRow, "узкий ряд не перенёсся на вторую строку")
        assertTrue(squeezed.last() < roomy.last(), "обычный ряд не сжал последнюю плашку — сравнение неверно")
    }

    @Test
    fun `рабочий экран на широком окне стоит посередине и не шире предела`() {
        val cap = ContentWidths.workspace.value.toInt()
        listOf(WIDE_MONITOR, NARROW).forEach { window ->
            var width = 0
            var left = -1f
            RenderProbe(width = window, height = HEIGHT) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.contentWidth(ContentKind.Workspace).onGloballyPositioned {
                            width = it.size.width
                            left = it.positionInRoot().x
                        }
                    )
                }
            }.use { it.frame() }
            val expected = minOf(window, cap)
            assertEquals(expected, width, "ширина рабочего экрана в окне $window")
            assertEquals((window - expected) / 2f, left, "рабочий экран в окне $window не посередине")
        }
    }

    @Test
    fun `текст встаёт от левого края и не шире читаемого`() {
        var width = 0
        var left = -1f
        RenderProbe(width = WIDE_MONITOR, height = HEIGHT) {
            Box(modifier = Modifier.width(WIDE_MONITOR.dp)) {
                Box(
                    modifier = Modifier.contentWidth(ContentKind.Reading).onGloballyPositioned {
                        width = it.size.width
                        left = it.positionInRoot().x
                    }
                )
            }
        }.use { it.frame() }
        assertEquals(ContentWidths.reading.value.toInt(), width)
        assertEquals(0f, left)
    }

    private companion object {
        const val WIDE = 1180
        const val NARROW = 420
        const val WIDE_MONITOR = 2560
        const val HEIGHT = 400
        val FILTERS = listOf("Продажа", "Возврат", "Покупка", "Возврат покупки", "Внесение", "Изъятие", "Отклонённые")
    }
}
