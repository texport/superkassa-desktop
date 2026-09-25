package kz.mybrain.superkassa.data.log

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.port.LogBookState

/**
 * Журнал рабочего места — тот, что пишет [AppLog], — как книга для окна отладки.
 *
 * Строки, порог и режим читаются из того же состояния, в котором их
 * держит [AppLog]: второго хранилища здесь нет, и окно видит ровно то,
 * что уходит в файл.
 *
 * @param files куда владелец сохраняет показанные строки: место выбирает
 *   окно системы.
 */
class AppLogBook(private val files: SavedFiles) : LogBook {

    /** Журнал подменяется целиком (проверки, запуск с диска): книга следит за тем, что сейчас. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: Flow<LogBookState> = AppLog.journals.flatMapLatest { journal ->
        combine(journal.lines, journal.levels, AppLog.debugModes) { lines, level, debug ->
            LogBookState(lines, level, debug, AppLog.file?.toString())
        }
    }

    override fun chooseLevel(level: LogLevel) = AppLog.chooseLevel(level)

    override fun switchDebugMode(on: Boolean) = AppLog.switchDebugMode(on)

    override fun clear() = AppLog.clear()

    override suspend fun save(lines: List<LogEntry>, title: String) {
        val text = lines.joinToString("\n") { it.line() }
        runCatching { files.save(text.encodeToByteArray(), SAVED_NAME, title) }
            .onFailure { AppLog.warn(LogSource.App, "log not saved: ${it::class.simpleName}") }
    }

    private companion object {
        /** Имя файла, предложенное при сохранении из окна. */
        const val SAVED_NAME = "superkassa-log.txt"
    }
}
