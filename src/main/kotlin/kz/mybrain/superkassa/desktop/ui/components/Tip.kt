package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

/**
 * Подсказка у того, что уже стоит на экране.
 *
 * [InfoTip] объясняет раздел и ради этого ставит свой значок; здесь
 * объяснять нужно надпись, которая и так есть, — плашку состояния,
 * обрезанное наименование. Поэтому подсказка вешается на сам элемент
 * и показывается наведением, без второго знака рядом.
 *
 * @param text объяснение целиком.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { RichTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = { content() }
    )
}
