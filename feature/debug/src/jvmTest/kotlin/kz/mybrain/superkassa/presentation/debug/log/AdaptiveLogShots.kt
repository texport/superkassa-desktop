package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.SettingsMeasure
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.domain.debug.port.LogBookState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
            time = TIME.format(LocalDateTime.of(2026, 9, 23, 12, 0).plusSeconds(index.toLong())),
            level = LogLevel.entries[index % LogLevel.entries.size],
            source = LogSource.entries[index % LogSource.entries.size],
            text = "POST /kkm/kkm-1/receipt → 409 ${SettingsMeasure.LONG_ADDRESS} #$index",
            body = if (index % 3 == 0) """{"code":"SHIFT_EXPIRED","message":"${SettingsMeasure.LONG_ORG}"}""" else null
        )
    }

    @Composable
    private fun Body(language: Language) {
        val texts = textsOf(language).debug
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                val journal = LogUiState(LogBookState(entries))
                LogFilters(texts, journal, object : LogActions {})
                LogLines(journal.shown, texts, Modifier.weight(1f))
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
            println(
                "журнал $name: правый край кнопок $right из $width, за краем ${controls.count { it.right > width }}"
            )
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
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
