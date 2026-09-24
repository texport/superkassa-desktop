package kz.mybrain.superkassa.domain.shift.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Досылает в БФД то, что ждёт в очереди. */
class SendQueued(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(): Answer<Int> =
        kassa.askSeated(signed) { api, seat -> api.queue.retryFailed(seat.kkmId, seat.pin) }
}
