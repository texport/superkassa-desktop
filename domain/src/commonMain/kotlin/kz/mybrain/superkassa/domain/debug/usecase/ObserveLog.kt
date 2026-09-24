package kz.mybrain.superkassa.domain.debug.usecase

import kotlinx.coroutines.flow.Flow
import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.port.LogBookState

/** Журнал рабочего места, как он есть сейчас: строки, порог и режим отладки. */
class ObserveLog(private val book: LogBook) {
    operator fun invoke(): Flow<LogBookState> = book.state
}
