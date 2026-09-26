package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Поле окна для этого окна — по его классу ширины (см. [Spacing.windowMargin]).
 *
 * Ставит его каркас окна один раз вокруг раздела; шапка начинает заголовок
 * с него же, и начало заголовка совпадает с краем содержимого под ним.
 */
val windowMargin: Dp
    @Composable get() = Spacing.windowMargin(LocalWindowClass.current.width == WidthClass.Compact)

/**
 * Поле окна снизу, которое каркас поставил под разделом.
 *
 * Прикреплённое к краю окна — нижний лист Material 3 — это поле перекрывает:
 * лист лежит на краю окна, а не висит над ним. Без каркаса поля снизу нет.
 */
val LocalFrameBottom = staticCompositionLocalOf { Spacing.flush }

/**
 * Дотянуть элемент вниз на поле окна под разделом — для того, что по
 * Material 3 прикреплено к краю окна: нижнего листа.
 */
fun Modifier.bleedToWindowEdge(bottom: Dp): Modifier = layout { measurable, constraints ->
    val extra = bottom.roundToPx()
    val grown = if (constraints.hasBoundedHeight) {
        constraints.copy(minHeight = constraints.minHeight + extra, maxHeight = constraints.maxHeight + extra)
    } else {
        constraints
    }
    val placeable = measurable.measure(grown)
    layout(placeable.width, (placeable.height - extra).coerceAtLeast(0)) { placeable.place(0, 0) }
}
