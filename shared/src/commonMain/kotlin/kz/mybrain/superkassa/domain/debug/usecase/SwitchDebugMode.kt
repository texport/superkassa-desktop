package kz.mybrain.superkassa.domain.debug.usecase

import kz.mybrain.superkassa.domain.debug.port.LogBook

/** Режим отладки: с ним открывается и закрывается окно журнала. */
class SwitchDebugMode(private val book: LogBook) {
    operator fun invoke(on: Boolean) = book.switchDebugMode(on)
}
