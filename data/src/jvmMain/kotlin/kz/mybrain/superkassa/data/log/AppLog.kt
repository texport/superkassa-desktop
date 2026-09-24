package kz.mybrain.superkassa.data.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.files.Path
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import java.io.File

/**
 * Журнал приложения, доступный отовсюду.
 *
 * Одно место на всё приложение: обмен с кассой и с кабинетом, отказы
 * подписи и переходы состояния кассы пишутся сюда, а не каждый в своё
 * место. Иначе разбор отказа означает сложить три источника по времени
 * руками.
 *
 * До [start] журнал живёт только в памяти и файла не заводит: так его
 * можно записывать из проверок, не трогая настоящую машину.
 */
object AppLog {

    private var settings: LogSettings? = null

    private val current = MutableStateFlow(LogJournal())

    private val debug = MutableStateFlow(false)

    /** Журнал рабочего места потоком: проверки подменяют его своим. */
    val journals: StateFlow<LogJournal> = current.asStateFlow()

    /**
     * Режим отладки потоком: при нём рядом с главным окном открыто окно
     * журнала. Выбор держится рабочего места: разбор отказа идёт не за
     * один запуск.
     */
    val debugModes: StateFlow<Boolean> = debug.asStateFlow()

    /** Журнал рабочего места. Подменяется проверками на свой, без файла. */
    var journal: LogJournal
        get() = current.value
        set(value) {
            current.value = value
        }

    /** Включён ли режим отладки сейчас. */
    val debugMode: Boolean get() = debug.value

    /** Поднимает журнал рабочего места: уровень, файл и режим отладки с диска. */
    fun start(loaded: LogSettings = LogSettings()) {
        settings = loaded
        journal = LogJournal(file = LogFile(Path(loaded.directory.path)), level = loaded.level)
        debug.value = loaded.debugMode
    }

    /** Порог записи, выбранный в настройках. */
    val level: LogLevel get() = journal.level

    /** Строки журнала от старой к новой. */
    val entries: List<LogEntry> get() = journal.entries

    /** Текущий файл журнала; до [start] его нет. */
    val file: File? get() = settings?.let { File(it.directory, LogFile.NAME) }

    fun chooseLevel(chosen: LogLevel) {
        journal.level = chosen
        settings?.level = chosen
    }

    fun switchDebugMode(on: Boolean) {
        debug.value = on
        settings?.debugMode = on
    }

    fun clear() = journal.clear()

    /** Записывает событие; тело попадёт в журнал только на отладочном уровне. */
    fun record(source: LogSource, level: LogLevel, text: String, body: String? = null) =
        journal.record(source, level, text, body)

    /** Переход состояния кассы: вход, смена, блокировка. */
    fun state(text: String) = record(LogSource.Machine, LogLevel.Info, text)

    /** Отказ подписи: NCALayer не ответил, владелец закрыл окно, сертификат отвергнут. */
    fun signatureRefused(text: String) = record(LogSource.Signature, LogLevel.Failure, text)

    /** Работа идёт не так, как задумано, но продолжается. */
    fun warn(source: LogSource, text: String) = record(source, LogLevel.Warning, text)
}
