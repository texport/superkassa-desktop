package kz.mybrain.superkassa.data.log

import kz.mybrain.superkassa.domain.debug.model.LogLevel as JournalLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource as JournalSource

/**
 * Имена уровня и источника записи там, где их ещё берут из `data.log`.
 *
 * Уровень, источник и строка журнала — понятия предметные и живут
 * в `domain.journal`: их читает окно отладки, а ему `data` видеть нельзя.
 * Файлы других областей, пишущие в журнал, пока берут их отсюда; имена
 * уйдут, когда эти файлы переведут на `domain.journal`.
 */
typealias LogLevel = JournalLevel

/** См. [LogLevel]. */
typealias LogSource = JournalSource
