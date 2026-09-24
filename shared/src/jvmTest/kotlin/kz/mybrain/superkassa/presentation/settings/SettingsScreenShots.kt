package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.desk
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки экрана настроек: из кассы и с экрана входа.
 *
 * Смотрит человек: группы названы своим хозяйством — касса, службы,
 * программа, — необратимое стоит последним, а до входа остаётся только
 * то, что задают раньше, чем куда-либо входят.
 *
 * Столбец настроек длиннее окна, поэтому снимков несколько: экран
 * прокручивается колесом, как его листает владелец.
 */
class SettingsScreenShots {

    @Composable
    private fun Screen(desk: KassaDesk) {
        Surface(Modifier.fillMaxSize()) { SettingsScreen(SettingsScene.board(desk)) }
    }

    /** Снимки экрана сверху донизу: кадр, прокрутка, следующий кадр. */
    private fun roll(name: String, desk: KassaDesk, screens: Int) {
        RenderProbe(width = WIDTH, height = HEIGHT) { Screen(desk) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            repeat(screens) { at ->
                File("/tmp/kassa-$name-$at.png").writeBytes(probe.frame())
                probe.wheel(at = Offset(600f, 400f), ticks = TICKS)
            }
        }
    }

    @Test
    fun `настройки администратора в кассе`() {
        val desk = KassaScene.desk()
        roll("settings-admin", desk, SCREENS)
        assertTrue(File("/tmp/kassa-settings-admin-0.png").length() > 0, "снимок настроек пуст")
    }

    @Test
    fun `настройки до входа`() {
        val desk = KassaScene.desk(kkm = null)
        roll("settings-door", desk, DOOR_SCREENS)
        assertTrue(File("/tmp/kassa-settings-door-0.png").length() > 0, "снимок настроек до входа пуст")
    }

    private companion object {
        const val WIDTH = 1180
        const val HEIGHT = 820
        const val SETTLE = 12

        /** Сколько кадров нужно, чтобы пройти столбец настроек до конца. */
        const val SCREENS = 12
        const val DOOR_SCREENS = 3

        /** Один ход колеса на экран: столько тиков прокручивают его целиком. */
        const val TICKS = 40f
    }
}
