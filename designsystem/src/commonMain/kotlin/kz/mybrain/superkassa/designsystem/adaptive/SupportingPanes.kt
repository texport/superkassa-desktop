package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.PaneSplit
import kz.mybrain.superkassa.designsystem.theme.size.Panes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Главная панель и вспомогательная, которая сворачивается до своей сводки,
 * — «вспомогательная панель» Material 3 (Canonical layouts → Supporting pane).
 *
 * С расширенного окна, где рядом обеим хватает места ([split]),
 * вспомогательная стоит сбоку
 * и убирается кнопкой: главная забирает всю ширину, а сводка встаёт под
 * ней во всю ширину. Уже — телефон, планшет стоймя, — вспомогательная
 * лежит снизу стандартным нижним листом ([SupportingSheet]): свёрнутый,
 * он показывает одну сводку, развёрнутый — всё. Сводка видна всегда:
 * в ней то, без чего работу не закончить, — например, итог и «Пробить чек».
 *
 * @param expanded вспомогательная развёрнута.
 * @param onToggle свернуть или развернуть её — кнопкой, жестом листа.
 * @param summary сводка; ей отдаётся кнопка «свернуть / развернуть», чтобы
 *   она стояла в её строке.
 */
@Composable
fun SupportingPanes(
    split: PaneSplit,
    expanded: Boolean,
    onToggle: () -> Unit,
    main: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    summary: @Composable (toggle: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val wide = LocalWindowClass.current.width >= WidthClass.Expanded
        val widths = sideBySide(split, maxWidth, Panes.gap)?.takeIf { wide }
        if (widths == null) {
            SupportingSheet(expanded, onToggle, main, supporting, summary)
        } else {
            val toggle: @Composable () -> Unit = {
                PanelToggle(if (expanded) AppIcons.sidePanelHide else AppIcons.sidePanelShow, expanded, onToggle)
            }
            if (expanded) {
                Beside(widths, main) { SideColumn(supporting, summary, toggle) }
            } else {
                Under(main) { summary(toggle) }
            }
        }
    }
}

/** Вспомогательная сбоку: над сводкой — её прокручиваемая часть. */
@Composable
private fun SideColumn(
    supporting: @Composable () -> Unit,
    summary: @Composable (@Composable () -> Unit) -> Unit,
    toggle: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        Box(modifier = Modifier.weight(1f)) { supporting() }
        summary(toggle)
    }
}

/** Обе панели рядом, по долям [widths]. */
@Composable
private fun Beside(widths: Pair<Dp, Dp>, main: @Composable () -> Unit, side: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(widths.first).fillMaxHeight()) { main() }
        Spacer(modifier = Modifier.width(Panes.gap))
        Box(modifier = Modifier.width(widths.second).fillMaxHeight()) { side() }
    }
}

/** Вспомогательная убрана: главная во всю ширину, сводка — под ней. */
@Composable
private fun Under(main: @Composable () -> Unit, summary: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { main() }
        summary()
    }
}

/** Кнопка «свернуть / развернуть» вспомогательной панели. */
@Composable
internal fun PanelToggle(icon: ImageVector, expanded: Boolean, onToggle: () -> Unit) {
    val texts = LocalStrings.current.general
    IconButton(onClick = onToggle) {
        Icon(icon, contentDescription = if (expanded) texts.collapse else texts.expand)
    }
}
