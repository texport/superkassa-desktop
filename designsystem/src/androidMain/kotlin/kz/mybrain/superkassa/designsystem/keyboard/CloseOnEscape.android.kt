package kz.mybrain.superkassa.designsystem.keyboard

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** «Назад» закрывает верхнее наложение: последний поставленный обработчик и слышит жест. */
@Composable
actual fun CloseOnEscape(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
}
