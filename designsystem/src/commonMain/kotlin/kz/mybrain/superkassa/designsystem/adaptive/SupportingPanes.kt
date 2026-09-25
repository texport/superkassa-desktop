package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.theme.size.PaneSplit
import kz.mybrain.superkassa.designsystem.theme.size.Panes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Главная панель и вспомогательная — «вспомогательная панель» Material 3
 * (Canonical layouts → Supporting pane).
 *
 * С расширенного окна, где рядом обеим хватает места ([split]),
 * вспомогательная стоит справа, над своей сводкой. Уже — телефон,
 * планшет стоймя, — она лежит снизу стандартным нижним листом
 * ([SupportingSheet]), который тянут за ручку: свёрнутый, он показывает
 * одну сводку, развёрнутый — всё. Сводка видна всегда: в ней то, без чего
 * работу не закончить, — например, итог и «Пробить чек».
 *
 * @param expanded нижний лист развёрнут.
 * @param onToggle лист развернули или свернули жестом.
 */
@Composable
fun SupportingPanes(
    split: PaneSplit,
    expanded: Boolean,
    onToggle: () -> Unit,
    main: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    summary: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val wide = LocalWindowClass.current.width >= WidthClass.Expanded
        val widths = sideBySide(split, maxWidth, Panes.gap)?.takeIf { wide }
        if (widths == null) {
            SupportingSheet(expanded, onToggle, main, supporting, summary)
        } else {
            Beside(widths, main) { SideColumn(supporting, summary) }
        }
    }
}

/**
 * Вспомогательная сбоку: над сводкой — её прокручиваемая часть.
 *
 * Сводка берёт свою высоту, но не больше доли [Panes.STACKED_SECOND_SHARE]
 * колонки, — остальное прокручиваемой части: развёрнутая сводка с пятью
 * видами оплаты иначе выталкивала за край окна поле штрихкода. Не
 * поместившееся сводка прокручивает сама.
 */
@Composable
private fun SideColumn(supporting: @Composable () -> Unit, summary: @Composable () -> Unit) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box { supporting() }
            Box { summary() }
        }
    ) { measurables, constraints ->
        val gap = Spacing.cardGap.roundToPx()
        val width = constraints.maxWidth
        val cap = (constraints.maxHeight * Panes.STACKED_SECOND_SHARE).toInt()
        val low = measurables[1].measure(Constraints(minWidth = width, maxWidth = width, maxHeight = cap))
        val rest = (constraints.maxHeight - low.height - gap).coerceAtLeast(0)
        val top = measurables[0].measure(Constraints.fixed(width, rest))
        layout(width, constraints.maxHeight) {
            top.place(0, 0)
            low.place(0, constraints.maxHeight - low.height)
        }
    }
}

/**
 * Обе панели рядом, по долям [widths]. Пока вспомогательная стоит справа,
 * снекбар окна встаёт по центру главной ([SnackbarRoom]).
 */
@Composable
private fun Beside(widths: Pair<Dp, Dp>, main: @Composable () -> Unit, side: @Composable () -> Unit) {
    val room = LocalSnackbarRoom.current
    val taken = widths.second + Panes.gap
    DisposableEffect(room, taken) {
        room.end = taken
        onDispose { room.end = Spacing.flush }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(widths.first).fillMaxHeight()) { main() }
        Spacer(modifier = Modifier.width(Panes.gap))
        Box(modifier = Modifier.width(widths.second).fillMaxHeight()) { side() }
    }
}
