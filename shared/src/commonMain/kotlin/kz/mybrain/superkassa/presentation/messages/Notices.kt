package kz.mybrain.superkassa.presentation.messages

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Строка сообщений окна: одна на всё приложение.
 *
 * Отказ и итог действия пишет сюда тот, кто действовал, — модель экрана
 * или прежний сеанс, — а показывает каркас окна, снекбаром внизу. Своей
 * строки у экрана нет: два снекбара одновременно кассир не прочтёт.
 */
class Notices {
    private val line = MutableStateFlow<Message?>(null)

    /** Что показано сейчас; `null` — ничего. */
    val current: StateFlow<Message?> = line.asStateFlow()

    /** Последнее сказанное; то же, что [current], для чтения без подписки. */
    val last: Message? get() = line.value

    fun show(message: Message) {
        line.value = message
    }

    /** Снимает строку: её прочли, или она больше не о том, что делается. */
    fun clear() {
        line.value = null
    }

    /** Снимает строку, только если на ней всё ещё [message]. */
    fun clear(message: Message) {
        line.compareAndSet(message, null)
    }
}
