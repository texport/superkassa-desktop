package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Объяснение раздела — под значком, а не строкой на экране.
 *
 * Абзац под каждой карточкой читают один раз при настройке, а место он
 * занимает всегда: на кассовом экране это две-три строки, которых не
 * хватает работе. По Material 3 такое объяснение живёт в подсказке
 * `RichTooltip`, вызываемой значком рядом с заголовком.
 *
 * Короткие правила поля — «4–8 цифр», «12 цифр» — сюда не переносятся:
 * их место под самим полем, в `supportingText`.
 *
 * @param text объяснение целиком.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTip(text: String) {
    val texts = LocalStrings.current
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { RichTooltip { Text(text) } },
        state = state
    ) {
        IconButton(onClick = { scope.launch { state.show() } }) {
            Icon(
                imageVector = AppIcons.info,
                contentDescription = texts.common.explain,
                modifier = Modifier.size(Sizes.infoIcon)
            )
        }
    }
}
