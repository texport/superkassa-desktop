package kz.mybrain.superkassa.presentation.debug.log

import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.matching
import kz.mybrain.superkassa.domain.debug.port.LogBookState

/**
 * Журнал, каким его видит владелец: книга и отбор в окне.
 *
 * Отбор живёт окном, а не рабочим местом: его меняют по ходу разбора
 * несколько раз за минуту, и помнить последний выбор до следующего
 * запуска незачем. Порог записи — другое дело: он в [book] и держится
 * рабочего места.
 *
 * @property filter уровень, с которого окно показывает строки.
 * @property query набранное в строке поиска.
 */
data class LogUiState(
    val book: LogBookState = LogBookState(),
    val filter: LogLevel = LogLevel.Debug,
    val query: String = ""
) {
    /** Строки, прошедшие отбор, — их же сохраняет кнопка «Сохранить». */
    val shown: List<LogEntry> get() = book.entries.matching(filter, query)
}
