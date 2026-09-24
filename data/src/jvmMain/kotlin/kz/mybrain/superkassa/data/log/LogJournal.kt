package kz.mybrain.superkassa.data.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Журнал приложения: одно место, куда пишется всё.
 *
 * Строки держатся в памяти для окна журнала и одновременно уходят в файл:
 * окно показывает происходящее сейчас, файл переживает перезапуск и
 * пересылается в поддержку.
 *
 * Уровень решает и что записывается, и насколько подробно: тела запросов
 * и ответов попадают в журнал только на отладочном уровне. Тайное
 * вырезается здесь, а не пишущим: полагаться на память пишущего в вопросе
 * пина и токена нельзя — см. [hideSecrets].
 *
 * @param capacity сколько строк держится в памяти; дальше уходят старые.
 * @param file куда писать на диск; без него журнал живёт только в памяти.
 * @param clock часы записи — задаются проверками.
 */
class LogJournal(
    private val capacity: Int = CAPACITY,
    private val file: LogFile? = null,
    level: LogLevel = LogLevel.Info,
    private val clock: () -> LocalDateTime = LocalDateTime::now
) {

    private val threshold = MutableStateFlow(level)

    private val records = MutableStateFlow<List<LogEntry>>(emptyList())

    /** Порог записи, выбранный в настройках, — потоком для окна отладки. */
    val levels: StateFlow<LogLevel> = threshold.asStateFlow()

    /** Строки журнала от старой к новой — потоком для окна отладки. */
    val lines: StateFlow<List<LogEntry>> = records.asStateFlow()

    /** Порог записи, выбранный в настройках. */
    var level: LogLevel
        get() = threshold.value
        set(value) {
            threshold.value = value
        }

    /** Строки журнала от старой к новой. */
    val entries: List<LogEntry> get() = records.value

    /**
     * Записывает событие.
     *
     * @param body тело запроса или ответа; попадает в журнал только
     *   на отладочном уровне.
     */
    fun record(source: LogSource, level: LogLevel, text: String, body: String? = null) {
        if (!level.passes(this.level)) return
        val entry = LogEntry(
            time = TIME.format(clock()),
            level = level,
            source = source,
            text = hideSecrets(text),
            body = body?.takeIf { this.level == LogLevel.Debug }?.let(::bodyForLog)
        )
        keep(entry)
        file?.append(entry.line())
    }

    /** Забывает записанное: окно чистят перед тем, как повторить отказ. */
    fun clear() {
        records.value = emptyList()
    }

    /** Старые строки уходят, когда память полна: всё, что старше, осталось в файле. */
    private fun keep(entry: LogEntry) = records.update { (it + entry).takeLast(capacity) }

    private companion object {

        /**
         * Сколько строк видно в окне.
         *
         * Двух тысяч хватает на разбор одного отказа целиком, а память
         * кассы они не занимают заметно. Всё, что старше, осталось в файле.
         */
        const val CAPACITY = 2000

        /** Время записи так, как его читают в окне и в файле. */
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
