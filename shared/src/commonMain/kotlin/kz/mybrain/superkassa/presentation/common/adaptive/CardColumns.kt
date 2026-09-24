package kz.mybrain.superkassa.presentation.common.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import kz.mybrain.superkassa.presentation.theme.size.CardGrid

/**
 * Карточки во всю доступную ширину — одним столбцом на узком окне и рядом
 * на широком.
 *
 * Столбцов столько, сколько позволяет класс окна ([cardColumns]), но не
 * больше, чем карточек, и не больше, чем помещается столбцов шириной
 * [CardGrid.columnMin]: карточки в панели рядом со списком встают так же,
 * как в разделе во всю ширину. Две карточки на очень большом окне делят
 * ширину пополам, а не оставляют третий столбец пустым.
 *
 * Каждая карточка встаёт в самый короткий на этот момент столбец: высоты
 * у карточек разные, и по очереди слева направо под короткой оставалась бы
 * пустота высотой в соседнюю.
 */
@Composable
fun CardColumns(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val byWindow = LocalWindowClass.current.width.cardColumns
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = CardGrid.gap.roundToPx()
        val width = constraints.maxWidth
        val fitting = (width + gap) / (CardGrid.columnMin.roundToPx() + gap)
        val columns = minOf(byWindow, measurables.size, fitting).coerceAtLeast(1)
        val column = ((width - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val tops = IntArray(columns)
        val placed = measurables.map { measurable ->
            val card = measurable.measure(Constraints.fixedWidth(column))
            val at = tops.indices.minBy { tops[it] }
            Stacked(card, at * (column + gap), tops[at]).also { tops[at] += card.height + gap }
        }
        val height = ((tops.maxOrNull() ?: 0) - gap).coerceAtLeast(0)
        layout(width, height) { placed.forEach { it.card.place(it.x, it.y) } }
    }
}

/** Карточка и её место в столбцах. */
private class Stacked(val card: Placeable, val x: Int, val y: Int)

/** Сколько столбцов карточек позволяет класс ширины окна. */
val WidthClass.cardColumns: Int
    get() = when (this) {
        WidthClass.Compact, WidthClass.Medium -> CardGrid.NARROW_COLUMNS
        WidthClass.Expanded, WidthClass.Large -> CardGrid.WIDE_COLUMNS
        WidthClass.ExtraLarge -> CardGrid.WIDEST_COLUMNS
    }
