package kz.mybrain.superkassa.presentation.theme

import androidx.compose.runtime.Composable

/** На телефоне и планшете полос прокрутки нет: тема ничего не добавляет. */
@Composable
internal actual fun PlatformTheme(content: @Composable () -> Unit) {
    content()
}
