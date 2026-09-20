package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Enter завершает ввод.
 *
 * За кассой набирают с клавиатуры, а штрихкоды и марки вводит сканер —
 * он присылает код и жмёт Enter сам. Поэтому Enter обязан работать
 * в каждом рабочем поле, и правило это одно на всё приложение: разойдись
 * оно по экранам, где-нибудь Enter перестал бы срабатывать, и кассир
 * тянулся бы к мыши посреди очереди.
 *
 * @param handle возвращает `true`, если нажатие принято; иначе оно уходит
 * дальше — например, к кнопке по умолчанию.
 */
fun Modifier.onEnter(handle: () -> Boolean): Modifier = onPreviewKeyEvent { event ->
    event.type == KeyEventType.KeyDown && event.key in ENTER_KEYS && handle()
}

/** Обычный Enter и Enter на цифровой части клавиатуры — одно и то же. */
private val ENTER_KEYS = setOf(Key.Enter, Key.NumPadEnter)
