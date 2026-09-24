package kz.mybrain.superkassa.presentation.common.keyboard

import androidx.compose.runtime.Composable

/**
 * Системный жест «назад» — шаг назад по экранам, а не закрытие наложения.
 *
 * Есть только там, где он есть у платформы: на Android это жест и кнопка
 * «назад», на настольной кассе такого жеста нет, и Escape по-прежнему
 * закрывает только наложения ([CloseOnEscape]) — переключать им разделы
 * значило бы уводить кассира с продажи случайным нажатием.
 *
 * Наложения, открытые позже, слышат жест первыми; пока [enabled] ложно,
 * жест уходит платформе — на Android это сворачивание приложения.
 */
@Composable
expect fun SystemBack(enabled: Boolean, onBack: () -> Unit)
