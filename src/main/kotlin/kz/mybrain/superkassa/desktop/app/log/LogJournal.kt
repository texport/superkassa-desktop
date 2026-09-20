package kz.mybrain.superkassa.desktop.app.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDateTime

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

    /** Порог записи, выбранный в настройках. */
    var level: LogLevel by mutableStateOf(level)

    private val records = mutableStateListOf<LogEntry>()

    /** Строки журнала от старой к новой. */
    val entries: List<LogEntry> get() = records

    /**
     * Записывает событие.
     *
     * @param body тело запроса или ответа; попадает в журнал только
     *   на отладочном уровне.
     */
    fun record(source: LogSource, level: LogLevel, text: String, body: String? = null) {
        if (!level.passes(this.level)) return
        val entry = LogEntry(
            at = clock(),
            level = level,
            source = source,
            text = hideSecrets(text),
            body = body?.takeIf { this.level == LogLevel.Debug }?.let(::bodyForLog)
        )
        keep(entry)
        file?.append(entry.line())
    }

    /** Забывает записанное: окно чистят перед тем, как повторить отказ. */
    @Synchronized
    fun clear() = records.clear()

    @Synchronized
    private fun keep(entry: LogEntry) {
        records.add(entry)
        while (records.size > capacity) {
            records.removeAt(0)
        }
    }

    private companion object {

        /**
         * Сколько строк видно в окне.
         *
         * Двух тысяч хватает на разбор одного отказа целиком, а память
         * кассы они не занимают заметно. Всё, что старше, осталось в файле.
         */
        const val CAPACITY = 2000
    }
}

/**
 * Отбор строк: уровень не ниже выбранного и совпадение с набранным.
 *
 * Отбор живёт здесь, а не в окне: это правило журнала, и проверяется оно
 * без запущенного интерфейса.
 */
fun List<LogEntry>.matching(level: LogLevel, query: String): List<LogEntry> =
    filter { it.level.passes(level) && it.matches(query) }
