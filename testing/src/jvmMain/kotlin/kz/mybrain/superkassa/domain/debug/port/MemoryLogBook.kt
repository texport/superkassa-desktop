package kz.mybrain.superkassa.domain.debug.port

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel

/**
 * Журнал для проверок: книга в памяти, без файла и без окна выбора.
 *
 * @property saved что просили сохранить в последний раз.
 */
class MemoryLogBook(initial: LogBookState = LogBookState()) : LogBook {
    override val state = MutableStateFlow(initial)

    var saved: List<LogEntry>? = null
        private set

    override fun chooseLevel(level: LogLevel) = state.update { it.copy(level = level) }

    override fun switchDebugMode(on: Boolean) = state.update { it.copy(debugMode = on) }

    override fun clear() = state.update { it.copy(entries = emptyList()) }

    override suspend fun save(lines: List<LogEntry>, title: String) {
        saved = lines
    }
}
