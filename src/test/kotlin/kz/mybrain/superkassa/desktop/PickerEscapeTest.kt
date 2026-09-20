package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.desktop.ui.components.escapePressed
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Escape закрывает раскрытый список.
 *
 * Выпадающий список Material 3 раскрывается в окне, которое фокус нарочно
 * не забирает — чтобы поле под ним оставалось набираемым. Нажатие поэтому
 * ловит само поле, и правило объявлено одно на все списки приложения:
 * разойдись оно по экранам, налоговый режим в настройках закрывался бы,
 * а выбор ОФД в мастере — нет.
 */
class PickerEscapeTest {

    @Test
    fun `закрывает нажатие Escape, а не отпускание и не другая клавиша`() {
        assertTrue(escapePressed(KeyEventType.KeyDown, Key.Escape))
        assertFalse(escapePressed(KeyEventType.KeyUp, Key.Escape))
        assertFalse(escapePressed(KeyEventType.KeyDown, Key.Enter))
        assertFalse(escapePressed(KeyEventType.KeyDown, Key.Spacebar))
    }

    @Test
    fun `нажатие доходит до поля, за которым раскрыт список`() {
        var closed = false
        RenderProbe(width = SIDE, height = SIDE) {
            val focus = FocusRequester()
            Box(modifier = Modifier.onEscape { closed = true; true }) {
                Box(modifier = Modifier.size(FIELD.dp).focusRequester(focus).focusable())
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }.use { probe ->
            probe.frame()
            probe.key(Key.Escape)
        }
        assertTrue(closed, "Escape не дошёл до поля с раскрытым списком")
    }

    @Test
    fun `пока закрывать нечего, нажатие уходит дальше`() {
        var seen = false
        RenderProbe(width = SIDE, height = SIDE) {
            val focus = FocusRequester()
            Box(modifier = Modifier.onEscape { seen = true; false }) {
                Box(modifier = Modifier.size(FIELD.dp).focusRequester(focus).focusable())
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }.use { probe ->
            probe.frame()
            probe.key(Key.Escape)
        }
        assertTrue(seen, "поле нажатия не увидело вовсе")
    }

    private companion object {
        const val SIDE = 200
        const val FIELD = 50
    }
}
