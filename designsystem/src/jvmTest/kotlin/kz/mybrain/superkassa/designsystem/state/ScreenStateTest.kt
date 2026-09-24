package kz.mybrain.superkassa.designsystem.state

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.motion.Durations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Общее место содержимого: ожидание, пустота, отказ и само содержимое.
 *
 * Проверяется то, ради чего элемент заведён один на всё приложение:
 * содержимое показывается только в готовом состоянии, а во всех
 * остальных на его месте стоит объяснение, а не пустота.
 */
class ScreenStateTest {

    private fun probe(state: ScreenState, onContent: () -> Unit = {}) = RenderProbe {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenSlot(state, Modifier.fillMaxSize()) {
                onContent()
                Text("содержимое")
            }
        }
    }

    @Test
    fun `готовое состояние показывает содержимое`() {
        var drawn = false
        probe(ScreenState.Ready) { drawn = true }.use { it.frame() }
        assertTrue(drawn, "в готовом состоянии рисуется то, ради чего экран открывали")
    }

    @Test
    fun `ожидание не пускает содержимое на экран`() {
        var drawn = false
        probe(ScreenState.Working) { drawn = true }.use { it.frame() }
        assertFalse(drawn, "пока ответа нет, показывать нечего")
    }

    @Test
    fun `пустота не пускает содержимое на экран`() {
        var drawn = false
        val empty = ScreenState.Empty(AppIcons.noDocuments, "Документов нет", "Пробейте чек")
        probe(empty).use { it.frame() }
        probe(empty) { drawn = true }.use { it.frame() }
        assertFalse(drawn)
    }

    @Test
    fun `отказ не пускает содержимое и повторяет только по нажатию`() {
        var drawn = false
        var retried = 0
        val trouble = ScreenState.Trouble("Служба не отвечает", "Проверьте связь") { retried += 1 }
        probe(trouble) { drawn = true }.use { it.frame() }
        assertFalse(drawn)
        assertEquals(0, retried, "повтор происходит по нажатию, а не сам собой")
    }

    /**
     * Мгновенный ответ не мигает.
     *
     * Кружок ждёт общей паузы, и первый кадр ожидания ничем не отличается
     * от пустой сцены: ответ, пришедший быстрее, снимает элемент раньше,
     * чем владелец что-то увидит.
     */
    @Test
    fun `кружок не успевает мелькнуть на первом кадре`() {
        assertTrue(Durations.beforeWaiting.inWholeMilliseconds > 0, "пауза одна на всё приложение")
        val working = probe(ScreenState.Working).use { it.frame() }
        val blank = RenderProbe { Column(modifier = Modifier.fillMaxSize()) {} }.use { it.frame() }
        assertContentSame(blank, working)
    }

    private fun assertContentSame(expected: ByteArray, actual: ByteArray) {
        assertTrue(expected.contentEquals(actual), "кадры разошлись: ${expected.size} против ${actual.size}")
    }
}
