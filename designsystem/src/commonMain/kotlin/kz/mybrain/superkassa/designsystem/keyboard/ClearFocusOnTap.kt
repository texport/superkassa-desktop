package kz.mybrain.superkassa.designsystem.keyboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Касание по пустому месту снимает фокус с поля ввода — и прячет
 * экранную клавиатуру.
 *
 * Встав в поиск, кассир не мог выйти из него касанием по экрану:
 * клавиатура закрывала половину списка, а убрать её можно было только
 * кнопкой «назад». Ставит это каркас окна один раз для всех экранов.
 * Касание, которое взял себе элемент — кнопка, строка списка, другое
 * поле, — до окна не доходит и фокуса не снимает.
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focus = LocalFocusManager.current
    return pointerInput(focus) { detectTapGestures(onTap = { focus.clearFocus() }) }
}
