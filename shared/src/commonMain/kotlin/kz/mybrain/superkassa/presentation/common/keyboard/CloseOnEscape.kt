package kz.mybrain.superkassa.presentation.common.keyboard

import androidx.compose.runtime.Composable

/**
 * Пока это наложение на экране, отмена платформы закрывает его: Escape
 * на настольной кассе, «назад» на Android.
 *
 * Ставится внутрь наложения: уходит из состава — снимается с учёта,
 * и отмена переходит к тому, что под ним.
 */
@Composable
expect fun CloseOnEscape(onClose: () -> Unit)
