package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.Swatch

/**
 * Выбор одного цвета из ряда — кружками, галочка на выбранном.
 *
 * Своя разметка, потому что готового составного у Material 3 нет:
 * гайдлайн описывает выбор цвета именно так — ряд закрашенных кругов
 * с отметкой на выбранном — и оставляет разметку приложению. Подпись
 * цвета живёт в подсказке и в описании для чтения с экрана: под каждым
 * кружком она удвоила бы высоту ряда ради того, что видно и так.
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        options.forEach { option ->
            Circle(
                swatch = swatch(option),
                label = label(option),
                selected = option == selected,
                onSelect = { onSelect(option) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Circle(swatch: Swatch, label: String, selected: Boolean, onSelect: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
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
