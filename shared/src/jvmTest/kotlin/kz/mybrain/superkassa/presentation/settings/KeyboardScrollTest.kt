package kz.mybrain.superkassa.presentation.settings

import androidx.compose.ui.input.key.Key
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
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
 *
 * Открытый раздел забирает ввод сам: выбрал раздел в списке — и PageDown
 * листает его, без Tab. Листается раздел печати: в нём девять своих строк
 * чека, и он длиннее окна.
 */
class KeyboardScrollTest {

    @Test
    fun `настройки прокручиваются PageDown`() {
        RenderProbe(WIDE, HIGH) { SettingsScreen(SettingsScene.board(KassaScene.desk())) }.use { probe ->
            openPrinting(probe)
            val top = probe.frame()
            File("/tmp/fix-9-settings-top.png").writeBytes(top)
            probe.key(Key.PageDown)
            val moved = probe.changedFrom(top)
            File("/tmp/fix-9-settings-pagedown.png").writeBytes(probe.frame())
            assertTrue(moved, "PageDown не сдвинул столбец настроек")
        }
    }

    @Test
    fun `настройки прокручиваются стрелкой вниз`() {
        RenderProbe(WIDE, HIGH) { SettingsScreen(SettingsScene.board(KassaScene.desk())) }.use { probe ->
            openPrinting(probe)
            val top = probe.frame()
            probe.key(Key.DirectionDown)
            assertTrue(probe.changedFrom(top), "стрелка вниз не сдвинула столбец настроек")
        }
    }

    /** Раздел печати открыт нажатием в списке, как его открывает владелец. */
    private fun openPrinting(probe: RenderProbe) {
        repeat(SETTLE) { probe.frame() }
        val printing = textsOf(Language.Ru).settings.sections.printing
        probe.tap { it.text == printing }
        repeat(SETTLE) { probe.frame() }
    }

    private companion object {
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 24
    }
}
