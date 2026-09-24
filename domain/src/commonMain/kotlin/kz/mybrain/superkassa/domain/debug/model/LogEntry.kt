package kz.mybrain.superkassa.domain.debug.model

/**
 * Одна строка журнала.
 *
 * Тело запроса или ответа лежит отдельным полем, а не приклеено к тексту:
 * в окне оно показывается под строкой и не мешает читать список, а отбор
 * по уровню и поиск работают по обоим.
 *
 * @property time время записи так, как его читают в окне и в файле:
 *   пишет его журнал, у которого есть часы машины.
 */
data class LogEntry(
    val time: String,
    val level: LogLevel,
    val source: LogSource,
    val text: String,
    val body: String? = null
) {

    /** Строка для файла и для сохранения из окна. */
    fun line(): String {
        val head = "$time ${level.code.uppercase()} ${source.code} $text"
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
}

/**
 * Отбор строк: уровень не ниже выбранного и совпадение с набранным.
 *
 * Отбор — правило журнала, а не окна: проверяется оно без запущенного
 * интерфейса.
 */
fun List<LogEntry>.matching(level: LogLevel, query: String): List<LogEntry> =
    filter { it.level.passes(level) && it.matches(query) }
