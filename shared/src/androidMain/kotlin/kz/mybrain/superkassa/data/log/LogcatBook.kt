package kz.mybrain.superkassa.data.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.port.LogBookState
import kotlin.time.Clock

/**
 * Журнал на Android глазами владельца.
 *
 * Записи уходят в журнал системы ([LogcatJournal]) и заодно ложатся сюда,
 * последними [KEPT] строками: их показывает журнал поверх кассы в режиме
 * отладки. Файла у кассы на Android нет — сохранять нечего. Порог и режим
 * помнятся до закрытия приложения.
 */
class LogcatBook : LogBook {
    private val current = MutableStateFlow(LogBookState(savable = false))

    override val state: StateFlow<LogBookState> = current.asStateFlow()

    override fun chooseLevel(level: LogLevel) = current.update { it.copy(level = level) }

    override fun switchDebugMode(on: Boolean) = current.update { it.copy(debugMode = on) }

    override fun clear() = current.update { it.copy(entries = emptyList()) }

    override suspend fun save(lines: List<LogEntry>, title: String) = Unit

    /** Запись журнала системы, прошедшая порог, — в показанные строки. */
    internal fun record(level: LogLevel, text: String) = current.update { now ->
        if (!level.passes(now.level)) return@update now
        val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        val entry = LogEntry(time.toString().take(TIME_LENGTH), level, LogSource.App, text)
        now.copy(entries = (now.entries + entry).takeLast(KEPT))
    }

    private companion object {
        /** Сколько последних строк держит память: разбор отказа — это минуты, а не день. */
        const val KEPT = 500

        /** Время строки — до секунд: «10:42:06». */
        const val TIME_LENGTH = 8
    }
}
