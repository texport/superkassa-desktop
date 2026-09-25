package kz.mybrain.superkassa.designsystem.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import kz.mybrain.superkassa.designsystem.list.bleed
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Выбор одного из немногих вариантов — строки списка Material 3 с радиокнопкой.
 *
 * По Material 3 (Radio button → Usage) радиокнопки берут, когда вариантов
 * мало и каждый нужно объяснить: подпись — строкой, объяснение — под ней,
 * а нажатие по любому месту строки выбирает вариант. Строки идут от края
 * до края колонки, а подписи начинаются с той же вертикали, что текст над
 * ними, — как у [SwitchRow].
 *
 * @param hint объяснение варианта под его подписью.
 */
@Composable
fun <T> RadioRows(
    options: List<T>,
    selected: T,
    title: (T) -> String,
    hint: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { option ->
            val chosen = option == selected
            ListItem(
                headlineContent = { Text(title(option)) },
                supportingContent = { Text(hint(option)) },
                leadingContent = { RadioButton(selected = chosen, onClick = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .bleed(Spacing.cardPadding)
                    .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(option) })
            )
        }
    }
}
