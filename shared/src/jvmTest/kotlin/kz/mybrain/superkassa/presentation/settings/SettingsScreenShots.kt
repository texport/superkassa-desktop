package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.settings.SettingsSectionTexts
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки экрана настроек: из кассы и с экрана входа, раздел за разделом.
 *
 * Смотрит человек: слева список разделов по полкам — касса, приложение,
 * кабинет БФД, — справа открытый раздел одной колонкой; до входа остаются
 * только разделы того, что задают раньше, чем куда-либо входят.
 *
 * Каждый раздел открывается нажатием в списке, как его открывает
 * владелец, и кадр снимается с ним; открытие меняет кадр — иначе
 * нажатие в списке ничего не открыло.
 */
class SettingsScreenShots {

    @Composable
    private fun Screen(desk: KassaDesk) {
        Surface(Modifier.fillMaxSize()) { SettingsScreen(SettingsScene.board(desk)) }
    }

    /** Кадр каждого раздела из [titles] — по названию в списке. */
    private fun sections(name: String, desk: KassaDesk, titles: (SettingsSectionTexts) -> List<String>) {
        val shown = titles(textsOf(Language.Ru).settings.sections)
        RenderProbe(width = WIDTH, height = HEIGHT) { Screen(desk) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            shown.forEachIndexed { at, title ->
                val before = probe.frame()
                probe.tap { it.text == title }
                repeat(SETTLE) { probe.frame() }
                val shot = File("/tmp/kassa-$name-$at.png")
                shot.writeBytes(probe.frame())
                assertTrue(shot.length() > 0, "снимок раздела «$title» пуст")
                if (at > 0) assertTrue(!before.contentEquals(probe.frame()), "раздел «$title» не открылся нажатием")
            }
        }
    }

    @Test
    fun `настройки администратора в кассе`() = sections("settings-admin", KassaScene.desk()) {
        with(it) {
            listOf(general, printing, taxes, bfd, look, language, salePanels, debug, about, connection)
        }
    }

    @Test
    fun `настройки до входа`() = sections("settings-door", KassaScene.desk(kkm = null)) {
        with(it) { listOf(look, language, salePanels, debug, about, connection) }
    }

    private companion object {
        const val WIDTH = 1180

        /** Высота, при которой список разделов виден целиком: нажимают по видимому. */
        const val HEIGHT = 1200
        const val SETTLE = 12
    }
}
