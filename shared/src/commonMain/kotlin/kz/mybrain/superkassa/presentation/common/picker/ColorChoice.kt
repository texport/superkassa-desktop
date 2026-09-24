package kz.mybrain.superkassa.presentation.common.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.theme.color.Swatch
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Выбор одного цвета из ряда — кружками, галочка на выбранном.
 *
 * Своя разметка, потому что готового составного у Material 3 нет:
 * гайдлайн описывает выбор цвета именно так — ряд закрашенных кругов
 * с отметкой на выбранном — и оставляет разметку приложению. Подпись
 * цвета живёт в подсказке и в описании для чтения с экрана: под каждым
 * кружком она удвоила бы высоту ряда ради того, что видно и так.
 *
 * Ряд переносится: цветов больше, чем помещается в узкую карточку, и
 * неперенесённый ряд обрезал бы последние кружки за правым краем —
 * выбрать их стало бы нечем. Строки при переносе равные.
 *
 * @param swatch заливка кружка и цвет отметки на ней — считаются
 *   в месте вызова, потому что зависят от нынешней темы.
 */
@Composable
fun <T> ColorChoice(
    options: List<T>,
    selected: T,
    swatch: @Composable (T) -> Swatch,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
            maxItemsInEachRow = evenRow(options.size, maxWidth)
        ) {
            Circles(options, selected, swatch, label, onSelect)
        }
    }
}

/**
 * Сколько кружков в строке, чтобы строки вышли поровну.
 *
 * Сколько помещается — столько и ставилось, и четырнадцатый тон уходил
 * сиротой на вторую строку. Строк столько же, сколько нужно, но кружки
 * делятся между ними поровну: семь и семь, а не тринадцать и один.
 */
private fun evenRow(count: Int, width: Dp): Int {
    val fit = ((width + Spacing.itemGap) / (Sizes.accentSwatch + Spacing.itemGap)).toInt().coerceAtLeast(1)
    val rows = (count + fit - 1) / fit
    return ((count + rows - 1) / rows).coerceAtLeast(1)
}

@Composable
private fun <T> Circles(
    options: List<T>,
    selected: T,
    swatch: @Composable (T) -> Swatch,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    options.forEach { option ->
        Circle(
            swatch = swatch(option),
            label = label(option),
            selected = option == selected,
            onSelect = { onSelect(option) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Circle(swatch: Swatch, label: String, selected: Boolean, onSelect: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.accentSwatch)
                .clip(CircleShape)
                .background(swatch.fill)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(AppIcons.chosen, contentDescription = label, tint = swatch.mark)
        }
    }
}
