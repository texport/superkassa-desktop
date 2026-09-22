package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

/**
 * Что закрывает Escape: верхнее из открытых наложений.
 *
 * Наложенные окна — печатная форма, карта во всё окно, диалоги — живут
 * в том же окне, что и разделы, а нажатие клавиши уходит тому, в чём
 * стоит фокус: обычно это поле или кнопка под наложением, и Escape
 * не доходил ни до одного из них. Поэтому клавишу слушает само окно,
 * а наложения записываются сюда, пока открыты; закрывается последнее
 * открытое — так же, как окна лежат друг на друге.
 */
object EscapeCloses {

    private val open = ArrayDeque<() -> Unit>()

    /** Нажат Escape: закрыть верхнее наложение. Ложь — закрывать нечего. */
    fun press(): Boolean {
        val top = open.lastOrNull() ?: return false
        top()
        return true
    }

    internal fun register(close: () -> Unit) {
        open.addLast(close)
    }

    internal fun unregister(close: () -> Unit) {
        open.remove(close)
    }
}

/**
 * Пока это наложение на экране, Escape закрывает его.
 *
 * Ставится внутрь наложения: уходит из состава — снимается с учёта,
 * и Escape переходит к тому, что под ним.
 */
@Composable
fun CloseOnEscape(onClose: () -> Unit) {
    val current by rememberUpdatedState(onClose)
    DisposableEffect(Unit) {
        val close: () -> Unit = { current() }
        EscapeCloses.register(close)
        onDispose { EscapeCloses.unregister(close) }
    }
}

/**
 * Слушает Escape у самого окна, ниже Compose.
 *
 * Наложения рисуются своим слоем и забирают ввод себе, а разделы под ними
 * его не видят; какой из слоёв услышит клавишу, решает фокус, и владелец,
 * ни на что не нажимавший, попадал мимо. Диспетчер клавиатуры видит
 * каждое нажатие в окне до всех слоёв.
 */
@Composable
fun EscapeListener() {
    DisposableEffect(Unit) {
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val dispatcher = KeyEventDispatcher { event ->
            event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE && EscapeCloses.press()
        }
        manager.addKeyEventDispatcher(dispatcher)
        onDispose { manager.removeKeyEventDispatcher(dispatcher) }
    }
}
