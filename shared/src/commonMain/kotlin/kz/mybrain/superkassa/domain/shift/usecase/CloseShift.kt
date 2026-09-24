package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Закрывает смену Z-отчётом; его не отменить. */
class CloseShift(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(): Answer<DeliveryStatus> =
        kassa.askSeated(signed) { api, seat -> api.closeShift(seat.kkmId, seat.pin).deliveryStatus }
}
