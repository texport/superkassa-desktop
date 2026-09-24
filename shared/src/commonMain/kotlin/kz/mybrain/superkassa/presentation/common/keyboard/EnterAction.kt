package kz.mybrain.superkassa.presentation.common.keyboard

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Что делает Enter в этой части экрана; `null` — ничего своего.
 *
 * [onEnter] ловит Enter клавиатуры и сканера, но экранная клавиатура
 * Android Enter не присылает: её клавиша действия («Готово», «Поиск»)
 * приходит полю как действие ввода. Без него кассир на планшете нажимал
 * «Готово» в поле штрихкода, клавиатура пряталась, а поиск не начинался.
 */
val LocalEnterAction = staticCompositionLocalOf<(() -> Boolean)?> { null }

/**
 * Клавиша действия экранной клавиатуры в полях [content] делает то же,
 * что Enter: [handle]. Сам Enter ловит [onEnter] на той же части экрана.
 */
@Composable
fun EnterSubmits(handle: () -> Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEnterAction provides handle, content = content)
}

/**
 * Действия экранной клавиатуры поля: клавиша действия — то же, что Enter
 * части экрана, где стоит поле. Вне [EnterSubmits] — поведение по умолчанию.
 */
@Composable
fun enterKeyboardActions(): KeyboardActions {
    val handle = LocalEnterAction.current ?: return KeyboardActions.Default
    return KeyboardActions(onDone = { handle() }, onGo = { handle() }, onSearch = { handle() }, onSend = { handle() })
}
