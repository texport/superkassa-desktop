package kz.mybrain.superkassa.data.log

import kotlinx.datetime.LocalDateTime
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Журнал ядра в журнале рабочего места.
 *
 * Без привязки SLF4J строки ядра об обмене с БФД пропадали: отказ
 * X-отчёта в журнале выглядел пустым местом, и причину было не найти.
 */
class CoreLoggerTest {

    private val before = AppLog.journal

    private val moment = LocalDateTime(2026, 9, 27, 10, 0, 0)

    private fun journal(level: LogLevel) =
        LogJournal(level = level, clock = { moment }).also { AppLog.journal = it }

    private val core = LoggerFactory.getLogger(
        "io.github.texport.superkassa.core.data.impl.adapter.ofd.OfdManagerAdapter"
    )

    @AfterTest
    fun restore() {
        AppLog.journal = before
    }

    @Test
    fun `отказ БФД из ядра ложится в журнал кассы`() {
        val log = journal(LogLevel.Info)
        core.warn("BFD {} failed: packet cannot be encoded or decoded", "X_REPORT")
        val line = log.entries.single()
        assertEquals(LogSource.Machine, line.source)
        assertEquals(LogLevel.Warning, line.level)
        assertEquals("[OfdManagerAdapter] BFD X_REPORT failed: packet cannot be encoded or decoded", line.text)
    }

    @Test
    fun `исключение пишется причиной, а не стеком`() {
        val log = journal(LogLevel.Info)
        val missing = IllegalArgumentException("openShiftTime missing")
        core.error("X-report failed", IllegalStateException("decode", missing))
        val text = log.entries.single().text
        val reason = ": IllegalStateException: decode ← IllegalArgumentException: openShiftTime missing"
        assertTrue(text.endsWith(reason), text)
    }

    @Test
    fun `отладочные строки ядра — только на отладочном уровне`() {
        val quiet = journal(LogLevel.Info)
        core.debug("BFD answered {} with code {}", "X_REPORT", 0)
        assertTrue(quiet.entries.isEmpty())
        val loud = journal(LogLevel.Debug)
        core.debug("BFD answered {} with code {}", "X_REPORT", 0)
        assertEquals("[OfdManagerAdapter] BFD answered X_REPORT with code 0", loud.entries.single().text)
    }
}
