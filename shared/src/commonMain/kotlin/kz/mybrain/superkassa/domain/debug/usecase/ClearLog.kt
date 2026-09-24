package kz.mybrain.superkassa.domain.debug.usecase

import kz.mybrain.superkassa.domain.debug.port.LogBook

/** Забывает показанное: окно чистят перед тем, как повторить отказ. */
class ClearLog(private val book: LogBook) {
    operator fun invoke() = book.clear()
}
