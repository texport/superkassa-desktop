package kz.mybrain.superkassa.designsystem.tip

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes

/**
 * Предупреждение под значком: что нельзя и почему.
 *
 * Отличается от объяснения ([InfoTip]) и видом, и смыслом: значок
 * предупреждения в цвете ожидания, а подсказка `RichTooltip` Material 3
 * несёт заголовок — что именно нельзя — и под ним почему. Так строка
 * списка не несёт плашки с длинной надписью, а предупреждение видно
 * с одного взгляда.
 *
 * @param title что нельзя — оно же подпись значка для чтения с экрана.
 * @param text почему и что делать.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningTip(title: String, text: String) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { RichTooltip(title = { Text(title) }) { Text(text) } },
        state = state
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                imageVector = AppIcons.warning,
                contentDescription = title,
                tint = StatusColors.pending,
                modifier = Modifier.size(Sizes.infoIcon)
            )
        }
    }
}
