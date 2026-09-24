package kz.mybrain.superkassa.domain.cabinet.usecase.applications

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn

/**
 * Сверяет сведения кассы этой машины с БФД после перерегистрации.
 *
 * Перерегистрация меняет торговую точку, а её адрес печатается в чеке.
 * Сверить можно только кассу, в которую вошли: пин принадлежит ей.
 *
 * @return ответ кассы; `null` — сверять нечего или нечем.
 */
class SyncServiceInfoHere(private val kassa: Kassa, private val signIn: SignIn) {
    suspend operator fun invoke(here: KkmResponse?): Answer<OfdCommandResponse>? {
        val seat = signIn.state.value
        val kkm = here?.takeIf { seat.signedIn && it.kkmId == seat.kkm?.kkmId } ?: return null
        return kassa.ask { it.syncOfdServiceInfo(kkm.kkmId, seat.pin) }
    }
}
