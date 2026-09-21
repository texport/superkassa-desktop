package kz.mybrain.superkassa.desktop.app.log

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Насколько важна запись журнала.
 *
 * Порядок здесь и есть порог записи: выбранный в настройках уровень
 * пропускает себя и всё, что важнее. Отладочный уровень — единственный,
 * на котором в журнал попадают тела запросов и ответов: на обычной работе
 * они раздувают файл и замедляют кассу, а при разборе отказа только они
 * и нужны.
 */
enum class LogLevel(val code: String) {

    /** Что именно ушло и что вернулось: разбор отказа по существу. */
    Debug("debug"),

    /** Обращение к узлу и к кабинету: метод, путь, код ответа, время. */
    Info("info"),

    /** Работа идёт, но не так, как задумано: повтор, пустой справочник. */
    Warning("warning"),

    /** Отказ: узел не ответил, кабинет отверг, подпись не получена. */
    Failure("failure");

    /** Попадает ли запись этого уровня в журнал при выбранном пороге. */
    fun passes(threshold: LogLevel): Boolean = ordinal >= threshold.ordinal

    companion object {
        fun byCode(code: String?): LogLevel = entries.firstOrNull { it.code == code } ?: Info
    }
}

/**
 * Откуда пришла запись.
 *
 * Источник назван коротко и одинаково в файле и в окне: по нему владелец
 * отделяет разговор с узлом от разговора с кабинетом, не вчитываясь
 * в текст строки.
 */
enum class LogSource(val code: String) {
    Node("node"),
    Cabinet("cabinet"),
    Machine("kkm"),
    Signature("eds"),
    App("app"),

    /**
     * Вывод самого узла, дочитанный из его файла.
     *
     * Отличается от [Node]: там обращение приложения к узлу, здесь —
     * что узел делал внутри, включая обмен с ОФД. Отдельным источником,
     * чтобы в окне отладки его можно было отобрать или убрать.
     */
    NodeSelf("node-self")
}

/**
 * Одна строка журнала.
 *
 * Тело запроса или ответа лежит отдельным полем, а не приклеено к тексту:
 * в окне оно показывается под строкой и не мешает читать список, а отбор
 * по уровню и поиск работают по обоим.
 */
data class LogEntry(
    val at: LocalDateTime,
    val level: LogLevel,
    val source: LogSource,
    val text: String,
    val body: String? = null
) {

    /** Время записи так, как его читают в окне и в файле. */
    fun time(): String = TIME.format(at)

    /** Строка для файла и для сохранения из окна. */
    fun line(): String {
        val head = "${time()} ${level.code.uppercase()} ${source.code} $text"
        return if (body == null) head else "$head\n    $body"
    }

    /** Подходит ли запись под набранное в строке поиска. */
    fun matches(query: String): Boolean {
        val wanted = query.trim()
        if (wanted.isEmpty()) return true
        return text.contains(wanted, ignoreCase = true) ||
            body?.contains(wanted, ignoreCase = true) == true ||
            source.code.contains(wanted, ignoreCase = true)
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
