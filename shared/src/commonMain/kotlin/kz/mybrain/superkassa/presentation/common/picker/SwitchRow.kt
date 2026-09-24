package kz.mybrain.superkassa.presentation.common.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Настройка «да или нет» строкой во всю ширину карточки.
 *
 * По Material 3 подпись стоит в начале строки, переключатель — в её конце,
 * и нажатие по любому месту строки переключает: попадать в сам переключатель
 * кассиру незачем. Прежде у каждой карточки была своя строка с переключателем
 * впереди подписи, и в широкой карточке они стояли столбиком у левого края.
 *
 * @param hint объяснение под значком у подписи; `null` — объяснять нечего.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onSwitch: (Boolean) -> Unit,
    enabled: Boolean = true,
    hint: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onSwitch),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Значок объяснения держится за подписью, а не уезжает к переключателю.
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
            hint?.let { InfoTip(it) }
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = null)
    }
}
