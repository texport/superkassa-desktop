package kz.mybrain.superkassa.domain.debug.port

import kotlinx.coroutines.flow.Flow
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel

/**
 * Журнал рабочего места глазами владельца: что записано и как пишется.
 *
 * [Journal] — перо, которым экраны пишут; здесь — сама книга: строки для
 * окна отладки, порог записи и режим отладки. Порог и режим держатся
 * рабочего места: разбор отказа редко укладывается в один запуск кассы.
 */
interface LogBook {

    /** Журнал сейчас; меняется с каждой записью и с каждым выбором. */
    val state: Flow<LogBookState>

    /** Порог записи: он решает, что попадёт в файл, и пишется файл и с закрытым окном. */
    fun chooseLevel(level: LogLevel)

    /** Режим отладки: с ним открывается и закрывается окно журнала. */
    fun switchDebugMode(on: Boolean)

    /** Забывает показанное: окно чистят перед тем, как повторить отказ. */
    fun clear()

    /**
     * Кладёт строки в файл, который выберет владелец.
     *
     * @param lines что сохранить — показанное после отбора.
     * @param title заголовок окна выбора файла.
     */
    suspend fun save(lines: List<LogEntry>, title: String)
}

/**
 * Журнал в одном снимке.
 *
 * @property entries строки от старой к новой.
 * @property file путь к файлу журнала; до запуска журнала файла нет.
 */
data class LogBookState(
    val entries: List<LogEntry> = emptyList(),
    val level: LogLevel = LogLevel.Info,
    val debugMode: Boolean = false,
    val file: String? = null
)
