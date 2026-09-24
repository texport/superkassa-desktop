package kz.mybrain.superkassa.domain.debug.usecase

import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.port.LogBook

/**
 * Порог записи журнала.
 *
 * Решает, что попадёт в файл, а файл пишется и с закрытым окном; по
 * умолчанию уровень обычный — обращения без тел.
 */
class ChooseLogLevel(private val book: LogBook) {
    operator fun invoke(level: LogLevel) = book.chooseLevel(level)
}
