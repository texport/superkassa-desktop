package kz.mybrain.superkassa.data.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.files.Path
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.port.LogBookState
import kotlin.time.Clock

/**
 * Журнал на Android глазами владельца.
 *
 * Записи уходят в журнал системы ([LogcatJournal]), ложатся сюда
 * последними [KEPT] строками — их показывает журнал поверх кассы в режиме
 * отладки — и пишутся в файл в каталоге приложения теми же правилами, что
 * на компьютере ([LogFile]): полмегабайта на файл и три прошлых. Файл
 * переживает перезапуск, и его пересылают в поддержку: показанное
 * сохраняется системным окном «Сохранить».
 *
 * Тайное вырезается перед записью ([hideSecrets]) — как на компьютере.
 *
 * @param directory каталог файлов журнала в памяти приложения.
 * @param screen активность на экране: окно «Сохранить» открывается поверх неё.
 */
class LogcatBook(directory: String, private val screen: ForegroundActivity) : LogBook {
    private val file = LogFile(Path(directory))
    private val current = MutableStateFlow(LogBookState(file = file.current.toString()))

    override val state: StateFlow<LogBookState> = current.asStateFlow()

    override fun chooseLevel(level: LogLevel) = current.update { it.copy(level = level) }

    override fun switchDebugMode(on: Boolean) = current.update { it.copy(debugMode = on) }

    override fun clear() = current.update { it.copy(entries = emptyList()) }

    override suspend fun save(lines: List<LogEntry>, title: String) {
        val target = screen.createDocument(SAVED_NAME, TEXT) ?: return
        val activity = screen.activity ?: return
        runCatching {
            activity.contentResolver.openOutputStream(target)?.use { out ->
                out.write(lines.joinToString("\n") { it.line() }.encodeToByteArray())
            }
        }
    }

    /** Запись журнала системы, прошедшая порог, — в показанные строки и в файл. */
    internal fun record(level: LogLevel, text: String) {
        if (!level.passes(current.value.level)) return
        val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        val entry = LogEntry(time.replace('T', ' ').take(TIME_LENGTH), level, LogSource.App, hideSecrets(text))
        current.update { now -> now.copy(entries = (now.entries + entry).takeLast(KEPT)) }
        file.append(entry.line())
    }

    private companion object {
        /** Сколько последних строк держит память: разбор отказа — это минуты, а не день. */
        const val KEPT = 500

        /** Время строки — как на компьютере, до тысячных: «2026-09-24 10:42:06.123». */
        const val TIME_LENGTH = 23

        /** Имя файла, предложенное при сохранении из окна. */
        const val SAVED_NAME = "superkassa-log.txt"

        const val TEXT = "text/plain"
    }
}
