package kz.mybrain.superkassa.presentation.common.keyboard

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Android: жест «назад» с предпросмотром — шаг назад, пока шагать есть куда. */
@Composable
actual fun SystemBack(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
