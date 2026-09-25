package kz.mybrain.superkassa.designsystem.picker

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import kz.mybrain.superkassa.designsystem.list.bleed
import kz.mybrain.superkassa.designsystem.theme.color.ContentAlpha
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Настройка «да или нет» — строка списка Material 3 с переключателем.
 *
 * По Material 3 подпись стоит в начале строки, объяснение — строкой под
 * ней, переключатель — в конце, и нажатие по любому месту строки
 * переключает: попадать в сам переключатель кассиру незачем. Строка идёт
 * от края до края панели, а подпись начинается с той же вертикали, что
 * подзаголовок и поля группы.
 *
 * @param hint объяснение под подписью; `null` — объяснять нечего.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onSwitch: (Boolean) -> Unit,
    enabled: Boolean = true,
    hint: String? = null
) {
    // Строка списка сама не гаснет: выключенную подпись приглушают так же,
    // как Material 3 приглушает выключенный переключатель рядом с ней.
    val shown = Modifier.alpha(if (enabled) ContentAlpha.FULL else ContentAlpha.DISABLED)
    ListItem(
        headlineContent = { Text(title, modifier = shown) },
        supportingContent = hint?.let { { Text(it, modifier = shown) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .bleed(Spacing.cardPadding)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onSwitch)
    )
}
