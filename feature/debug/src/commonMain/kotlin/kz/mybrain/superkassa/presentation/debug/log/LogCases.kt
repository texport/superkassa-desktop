package kz.mybrain.superkassa.presentation.debug.log

import kz.mybrain.superkassa.domain.debug.port.LogBook
import kz.mybrain.superkassa.domain.debug.usecase.ChooseLogLevel
import kz.mybrain.superkassa.domain.debug.usecase.ClearLog
import kz.mybrain.superkassa.domain.debug.usecase.ObserveLog
import kz.mybrain.superkassa.domain.debug.usecase.SaveLog
import kz.mybrain.superkassa.domain.debug.usecase.SwitchDebugMode

/** Сценарии журнала отладки: всё, что модель делает с журналом рабочего места. */
class LogCases(book: LogBook) {
    val observe = ObserveLog(book)
    val chooseLevel = ChooseLogLevel(book)
    val switchDebugMode = SwitchDebugMode(book)
    val clear = ClearLog(book)
    val save = SaveLog(book)
}
