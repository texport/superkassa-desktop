package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Открывает смену — внутренняя операция кассы, документа в БФД нет. */
class OpenShift(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(): Answer<ShiftResponse> =
        kassa.askSeated(signed) { api, seat -> api.openShift(seat.kkmId, seat.pin) }
}
