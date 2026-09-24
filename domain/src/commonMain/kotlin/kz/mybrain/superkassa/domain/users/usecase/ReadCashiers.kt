package kz.mybrain.superkassa.domain.users.usecase

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Кассиры кассы, за которой работают, — пином работающего. */
class ReadCashiers(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return кассиры, как их знает касса. */
    suspend operator fun invoke(): Answer<List<UserResponse>> =
        kassa.askSeated(signed) { api, seat -> api.listUsers(seat.kkmId, seat.pin) }
}
