package kz.mybrain.superkassa.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Клавиши однострочного поля у поля, которое переносит строку на вид.
 *
 * Своя строка чека — одна строка печати, а на сто знаков её не видно
 * в однострочном поле. Поле стало многострочным ради показа, но ввод
 * остался прежним: Enter перевода строки не вставляет, Tab уводит
 * к следующему полю, а не вставляет табуляцию. Иначе раскладка поменяла
 * бы то, что уходит на узел, и то, как по форме ходят с клавиатуры.
 */
@Composable
internal fun Modifier.keysOfSingleLine(): Modifier {
    val focus = LocalFocusManager.current
    return onPreviewKeyEvent { event ->
        when (event.key) {
            Key.Enter, Key.NumPadEnter -> true
            Key.Tab -> {
                if (event.type == KeyEventType.KeyDown) {
                    focus.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                }
                true
            }
            else -> false
        }
    }
}
