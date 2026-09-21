package kz.mybrain.superkassa.desktop

import androidx.compose.ui.input.key.Key
import kz.mybrain.superkassa.desktop.ui.settings.SettingsScreen
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Длинный столбец двигается с клавиатуры, а не только колесом.
 *
 * До нижних карточек настроек — режима отладки и сведений об узле —
 * нельзя было добраться ни PageDown, ни стрелками, ни средствами
 * доступности: колесо мыши было единственным способом. Настольное
 * приложение обязано работать с клавиатуры целиком.
 *
 * Проверяется тем же способом, что и прокрутка колесом: по изменению
 * картинки. Оно и есть ответ на вопрос «доехало ли содержимое».
 */
class KeyboardScrollTest {

    @Test
    fun `настройки прокручиваются PageDown`() {
        RenderProbe(WIDE, HIGH) { SettingsScreen(Look.session()) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val top = probe.frame()
            File("/tmp/fix-9-settings-top.png").writeBytes(top)
            // Обход клавиатурой начинается с Tab: пока ничего не выбрано,
            // нажатие приложению не достаётся вовсе — ни в окне, ни здесь.
            probe.key(Key.Tab)
            probe.key(Key.PageDown)
            val moved = probe.changedFrom(top)
            File("/tmp/fix-9-settings-pagedown.png").writeBytes(probe.frame())
            assertTrue(moved, "PageDown не сдвинул столбец настроек")
        }
    }

    @Test
    fun `настройки прокручиваются стрелкой вниз`() {
        RenderProbe(WIDE, HIGH) { SettingsScreen(Look.session()) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val top = probe.frame()
            probe.key(Key.Tab)
            probe.key(Key.DirectionDown)
            assertTrue(probe.changedFrom(top), "стрелка вниз не сдвинула столбец настроек")
        }
    }

    private companion object {
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 24
    }
}
