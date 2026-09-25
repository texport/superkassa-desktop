package kz.mybrain.superkassa.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.WindowSteps
import kz.mybrain.superkassa.desk
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.presentation.shell.bar.ShellBar
import kz.mybrain.superkassa.presentation.shell.frame.ShellFrame
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.SectionContent
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import kz.mybrain.superkassa.windowCabinet
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Кадры настроек в окне кассы на размерах приёмки: телефон, складной,
 * планшет стоймя и лёжа, ноутбук, настольный и большой монитор.
 *
 * Кадры — `/tmp/settings-frames/<окно>.png`: их смотрит человек. Где
 * раздел открывается поверх списка, снимается и он —
 * `/tmp/settings-frames/<окно>-section.png`: раздел печати, открытый
 * нажатием в списке.
 */
class SettingsFramesShots {

    @Composable
    private fun Window(desk: KassaDesk) {
        val shell by desk.parts.shell.state.collectAsState()
        Surface(Modifier.fillMaxSize()) {
            WindowSteps { step, back ->
                ShellFrame(
                    Section.entries,
                    Section.Settings,
                    {},
                    topBar = { onMenu -> ShellBar(desk.parts, shell, Section.Settings, {}, onMenu, back) }
                ) {
                    Box(Modifier.fillMaxSize().padding(it)) { Settings(desk, step as? SettingsSectionKey) }
                }
            }
        }
    }

    /**
     * Раздел настроек окна. Адреса кабинета и карты — на сотню знаков:
     * поле обязано показать их, не выталкивая кнопку за край.
     */
    @Composable
    private fun Settings(desk: KassaDesk, opened: SettingsSectionKey?) {
        val app = remember { SettingsScene.app(desk) }
        val parts = remember { WindowParts(desk.parts.shell, desk.look, windowCabinet(app, desk.look)) }
        ProvideWindowModels(remember { WindowModels() }) { SectionContent(app, parts, Section.Settings, opened) }
    }

    @Test
    fun `настройки на размерах приёмки`() {
        val folder = File(FOLDER).apply { mkdirs() }
        SIZES.forEach { (width, height) ->
            RenderProbe(width, height) { Window(KassaScene.desk(admin = true)) }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                val shot = File(folder, "${width}x$height.png")
                shot.writeBytes(probe.frame())
                assertTrue(shot.length() > 0, "кадр ${width}x$height пуст")
                probe.tap { it.text == textsOf(Language.Ru).settings.sections.printing }
                repeat(PANE_MOTION) { probe.frame() }
                File(folder, "${width}x$height-section.png").writeBytes(probe.frame())
            }
        }
    }

    private companion object {
        const val FOLDER = "/tmp/settings-frames"
        const val VERSION = "1.0.6"
        const val SETTLE = 20

        /** Кадров на движение панели: полсекунды с запасом. */
        const val PANE_MOTION = 60
        val SIZES = listOf(360 to 800, 411 to 891, 673 to 841, 800 to 1280, 1280 to 800, 1920 to 1080, 2560 to 1600)
    }
}
