package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.presentation.adaptive.MoneyText
import kz.mybrain.superkassa.presentation.adaptive.NumberText
import kz.mybrain.superkassa.presentation.adaptive.ScrollingTable
import kz.mybrain.superkassa.presentation.adaptive.TableColumn
import kz.mybrain.superkassa.presentation.adaptive.TableLine
import kz.mybrain.superkassa.presentation.adaptive.tableWidths
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.TableColumns
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Таблица с наименьшей шириной столбцов и прокруткой вбок.
 *
 * В журнале узкого окна кнопка печати в конце строки пропадала до точки:
 * у столбцов не было ни наименьшей ширины, ни прокрутки. Здесь кнопка
 * в узком окне стоит за правым краем целиком, своей ширины, и колесо
 * вбок приводит её на экран.
 *
 * Кадры остаются в `/tmp/table-*.png`.
 */
class ScrollingTableTest {

    private val columns = listOf(
        TableColumn(TableColumns.number, weight = 0f, numeric = true),
        TableColumn(TableColumns.moment, weight = 0f),
        TableColumn(TableColumns.name, weight = 1f),
        TableColumn(TableColumns.money, weight = 0f, numeric = true),
        TableColumn(TableColumns.action, weight = 0f)
    )

    @Test
    fun `лишнее место делится по весам, а тесное оставляет наименьшие ширины`() {
        val least = columns.sumOf { it.min.value.toDouble() }.toFloat()
        val roomy = tableWidths(columns, (least + EXTRA).dp)
        assertEquals((TableColumns.name.value + EXTRA), roomy[2].value, "лишнее ушло не названию")
        assertEquals(TableColumns.action, roomy[4], "кнопка выросла вместе с окном")
        assertEquals(columns.map { it.min }, tableWidths(columns, (least / 2).dp), "тесно — а столбцы сжались")
    }

    @Test
    fun `в узком окне кнопка строки целиком за краем и приезжает прокруткой вбок`() {
        var button = 0f to 0
        RenderProbe(width = NARROW, height = HEIGHT) {
            Box(modifier = Modifier.fillMaxSize()) {
                ScrollingTable(
                    columns = columns,
                    modifier = Modifier.fillMaxSize(),
                    header = { widths -> TableLine(widths) { Text(HEADERS[it]) } },
                    rows = { widths ->
                        items((1..ROWS).toList()) { row ->
                            TableLine(widths) { column -> Cell(row, column) { button = it } }
                        }
                    }
                )
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            File("/tmp/table-narrow.png").writeBytes(before)
            val hidden = button
            repeat(TURNS) { probe.wheel(at = Offset(NARROW / 2f, HEIGHT / 2f), ticks = WHEEL, across = true) }
            File("/tmp/table-narrow-scrolled.png").writeBytes(probe.frame())
            println("кнопка строки: до прокрутки x=${hidden.first} ширина ${hidden.second}, после x=${button.first}")
            assertTrue(hidden.first >= NARROW, "кнопка строки видна без прокрутки — таблица не шире окна")
            val own = (TableColumns.action - TableColumns.cellPadding * 2).value.toInt()
            assertEquals(own, hidden.second, "кнопка строки сжата")
            assertTrue(button.first + button.second <= NARROW, "прокрутка вбок не привела кнопку на экран")
        }
    }

    @Composable
    private fun Cell(row: Int, column: Int, onButton: (Pair<Float, Int>) -> Unit) {
        when (column) {
            0 -> NumberText(Money.count(row * BIG))
            1 -> Text("23.09.2026 18:0$row")
            2 -> Text("Товар с длинным названием $row")
            3 -> MoneyText(Money.formatTiyn(row * BIG * BIG))
            else -> IconButton(
                onClick = {},
                modifier = Modifier.onGloballyPositioned {
                    if (row == 1) onButton(it.positionInRoot().x to it.size.width)
                }
            ) { Icon(AppIcons.print, contentDescription = null) }
        }
    }

    private companion object {
        const val NARROW = 420
        const val HEIGHT = 360
        const val ROWS = 5
        const val EXTRA = 100f
        const val BIG = 12_345L
        const val SETTLE = 4
        const val WHEEL = 20f
        const val TURNS = 3
        val HEADERS = listOf("Номер", "Время", "Товар", "Сумма", "")
    }
}
