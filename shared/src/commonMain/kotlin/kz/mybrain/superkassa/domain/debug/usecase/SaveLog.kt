package kz.mybrain.superkassa.domain.debug.usecase

import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.port.LogBook

/** Сохраняет показанное в файл: в поддержку пересылают разбор одного отказа, а не всю смену. */
class SaveLog(private val book: LogBook) {
    suspend operator fun invoke(lines: List<LogEntry>, title: String) = book.save(lines, title)
}
