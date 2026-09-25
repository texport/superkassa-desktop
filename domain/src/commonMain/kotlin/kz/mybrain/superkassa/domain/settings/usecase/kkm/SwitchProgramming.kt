package kz.mybrain.superkassa.domain.settings.usecase.kkm

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Вход в режим программирования и выход из него.
 *
 * Касса принимает свои настройки и снятие только в этом режиме.
 */
class SwitchProgramming(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @param enter войти в режим; `false` — выйти из него. */
    suspend operator fun invoke(enter: Boolean): Answer<KkmResponse> = kassa.changeSeated(signed) { api, seat ->
        if (enter) api.enterProgramming(seat.kkmId, seat.pin) else api.exitProgramming(seat.kkmId, seat.pin)
    }
}
