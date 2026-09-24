package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Снимает X-отчёт открытой смены. */
class TakeXReport(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(): Answer<DeliveryStatus> =
        kassa.askSeated(signed) { api, seat -> api.createReport(seat.kkmId, seat.pin).deliveryStatus }
}
