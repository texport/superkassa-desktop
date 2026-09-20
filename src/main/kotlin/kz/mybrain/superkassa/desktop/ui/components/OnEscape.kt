package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Escape закрывает раскрытое.
 *
 * За кассой работают с клавиатуры, и раскрытый список закрывают ею же:
 * Escape — единственное общепринятое для этого нажатие. Выпадающий
 * список Material 3 (`ExposedDropdownMenuBox`) его не слышит сам —
 * список раскрывается в окне, которое фокус нарочно не забирает, чтобы
 * поле под ним оставалось набираемым. Поэтому нажатие ловит само поле,
 * и правило это объявлено здесь один раз: разойдись оно по экранам,
 * налоговый режим в настройках закрывался бы, а выбор ОФД в мастере — нет.
 *
 * Нажатие перехватывается только тогда, когда есть что закрывать: иначе
 * Escape уходит дальше — к диалогу, который он и должен закрыть.
 *
 * @param handle возвращает `true`, если нажатие принято.
 */
fun Modifier.onEscape(handle: () -> Boolean): Modifier = onPreviewKeyEvent { event ->
    escapePressed(event.type, event.key) && handle()
}

/**
 * Нажатие, которым закрывают раскрытое.
 *
 * Считается только нажатие, а не отпускание: иначе Escape, закрывший
 * диалог, тут же закрывал бы и то, что под ним открылось.
 */
internal fun escapePressed(type: KeyEventType, key: Key): Boolean =
    type == KeyEventType.KeyDown && key == Key.Escape
