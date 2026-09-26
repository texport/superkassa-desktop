package kz.mybrain.superkassa.designsystem.tip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch

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
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { RichTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = { content() }
    )
}

/**
 * Подсказка, которая открывается нажатием на сам элемент.
 *
 * Наведение есть только у мыши: на планшете плашка «Смена открыта»
 * с подсказкой по наведению не объяснялась ничем. Нажатие открывает
 * подсказку и на экране, и мышью; наведение мышью — тоже.
 *
 * @param text объяснение целиком.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapTip(text: String, content: @Composable () -> Unit) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { RichTooltip { Text(text) } },
        state = state
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(onClickLabel = text) { scope.launch { state.show() } }
        ) { content() }
    }
}
