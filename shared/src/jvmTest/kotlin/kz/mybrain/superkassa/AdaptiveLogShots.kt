package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.data.log.LogEntry
import kz.mybrain.superkassa.data.log.LogLevel
import kz.mybrain.superkassa.data.log.LogSource
import kz.mybrain.superkassa.presentation.debug.LogFilters
import kz.mybrain.superkassa.presentation.debug.LogLines
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.debugTexts
import kz.mybrain.superkassa.presentation.theme.Appearance
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import java.io.File
import java.time.LocalDateTime
import kotlin.test.Test

/**
 * Окно журнала отладки на двух тысячах строк: кадры и замеры.
 *
 * Кадры — `/tmp/adaptive-settings-log-<окно>-<язык>-<ступень>.png`;
 * в выводе — правый край кнопок против ширины окна.
 */
class AdaptiveLogShots {

    private val entries = List(LINES) { index ->
        LogEntry(
            at = LocalDateTime.of(2026, 9, 23, 12, 0).plusSeconds(index.toLong()),
            level = LogLevel.entries[index % LogLevel.entries.size],
            source = LogSource.entries[index % LogSource.entries.size],
            text = "POST /kkm/kkm-1/receipt → 409 ${SettingsMeasure.LONG_ADDRESS} #$index",
            body = if (index % 3 == 0) """{"code":"SHIFT_EXPIRED","message":"${SettingsMeasure.LONG_ORG}"}""" else null
        )
    }

    @Composable
    private fun Body(language: Language) {
        val texts = debugTexts(language)
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                LogFilters(texts, LogLevel.Debug, "", entries.size, {}, {}, {}, {})
                LogLines(entries, texts, Modifier.weight(1f))
            }
        }
    }

    private fun shoot(width: Int, height: Int, language: Language, scale: TextScale) {
        val name = "${width}x$height-${language.name.lowercase()}-${scale.name.lowercase()}"
        val theme = if (width == DARK_WIDTH) Appearance.Dark else Appearance.Light
        RenderProbe(width, height, theme, Look(textScale = scale), language) { Body(language) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/adaptive-settings-log-$name.png").writeBytes(probe.frame())
            val controls = SettingsMeasure.controls(probe.semantics())
            val right = controls.maxOfOrNull { it.right }
            println("журнал $name: правый край кнопок $right из $width, за краем ${controls.count { it.right > width }}")
        }
    }

    @Test
    fun `журнал отладки на двух тысячах строк`() {
        SIZES.forEach { (width, height) ->
            listOf(Language.Ru, Language.Kk).forEach { language ->
                listOf(TextScale.Normal, TextScale.Larger).forEach { scale -> shoot(width, height, language, scale) }
            }
        }
    }

    private companion object {
        const val LINES = 2000
        const val SETTLE = 20
        const val DARK_WIDTH = 1920
        val SIZES = listOf(920 to 640, 960 to 640, 1920 to 1080, 600 to 640)
    }
}
