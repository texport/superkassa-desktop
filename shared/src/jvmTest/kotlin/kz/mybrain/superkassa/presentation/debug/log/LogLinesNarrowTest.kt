package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.debug.debugTexts
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Строки журнала отладки на телефоне 360×800.
 *
 * Колонки уровня и источника постоянной ширины съедали строку телефона,
 * и текст записи вытягивался в столбик по слову. Меряется ширина текста
 * записи: не меньше половины экрана. Кадр — `/tmp/narrow-log.png`.
 */
class LogLinesNarrowTest {
    private val entries = List(LINES) { index ->
        LogEntry(
            time = "2026-09-24 12:00:0$index.000",
            level = LogLevel.entries[index % LogLevel.entries.size],
            source = LogSource.entries[index % LogSource.entries.size],
            text = "$TEXT #$index"
        )
    }

    @Test
    fun `текст записи на телефоне занимает строку, а не столбик`() {
        RenderProbe(WIDTH, HEIGHT) {
            Surface(Modifier.fillMaxSize()) { LogLines(entries, debugTexts(Language.Ru), Modifier.fillMaxSize()) }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/narrow-log.png").writeBytes(probe.frame())
            val lines = probe.nodes().filter { it.text.startsWith(TEXT) }
            assertTrue(lines.isNotEmpty(), "строк журнала не видно")
            lines.forEach { assertTrue(it.width >= WIDTH / 2, "текст записи сжат в столбик: $it") }
        }
    }

    private companion object {
        const val WIDTH = 360
        const val HEIGHT = 800
        const val LINES = 6
        const val SETTLE = 5
        const val TEXT = "Receipt accepted by the BFD"
    }
}
