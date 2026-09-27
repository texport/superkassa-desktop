package kz.mybrain.superkassa.data.log

import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter

/**
 * Одна метка журнала ядра: строки её класса ложатся в журнал рабочего
 * места под источником кассы.
 *
 * Порог — тот же, что выбран в настройках журнала: отладочные строки
 * ядра («отправлено в БФД», «БФД ответил кодом») видны на отладочном
 * уровне, предупреждения и отказы — всегда. Тайное вырезает сам журнал.
 *
 * @param name полное имя класса ядра; в строке — только короткое.
 */
internal class CoreLogger(name: String) : LegacyAbstractLogger() {

    init {
        this.name = name.substringAfterLast('.')
    }

    override fun isTraceEnabled(): Boolean = LogLevel.Debug.passes(AppLog.level)

    override fun isDebugEnabled(): Boolean = LogLevel.Debug.passes(AppLog.level)

    override fun isInfoEnabled(): Boolean = LogLevel.Info.passes(AppLog.level)

    override fun isWarnEnabled(): Boolean = LogLevel.Warning.passes(AppLog.level)

    override fun isErrorEnabled(): Boolean = LogLevel.Failure.passes(AppLog.level)

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?
    ) {
        val text = MessageFormatter.basicArrayFormat(messagePattern, arguments)
        AppLog.record(LogSource.Machine, level.journalLevel(), "[$name] $text${throwable.reason()}")
    }
}

/** Уровень SLF4J — уровнем журнала рабочего места. */
private fun Level.journalLevel(): LogLevel = when (this) {
    Level.ERROR -> LogLevel.Failure
    Level.WARN -> LogLevel.Warning
    Level.INFO -> LogLevel.Info
    Level.DEBUG, Level.TRACE -> LogLevel.Debug
}

/**
 * Причина отказа одной строкой: класс и сообщение исключения и его
 * первопричины. Стек в журнал не идёт — владелец пересылает файл
 * в поддержку, и читать его должен человек.
 */
private fun Throwable?.reason(): String {
    val failure = this ?: return ""
    val root = generateSequence(failure) { it.cause }.last()
    val own = ": ${failure::class.simpleName}: ${failure.message.orEmpty()}"
    return if (root === failure) own else "$own ← ${root::class.simpleName}: ${root.message.orEmpty()}"
}
