package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn

/**
 * Вписывает выданный токен в кассу этой машины.
 *
 * БФД отзывает прежний токен, и касса, о новом не знающая, на первом же чеке
 * получила бы «неверный токен» и блокировку. Вписать можно только в ту кассу,
 * в которую вошли: пин принадлежит ей.
 *
 * @return ответ кассы; `null` — вписывать некуда: касса не здешняя или вошли в другую.
 */
class WriteTokenHere(private val kassa: Kassa, private val signIn: SignIn) {
    suspend operator fun invoke(here: KkmResponse?, token: String): Answer<KkmResponse>? {
        val seat = signIn.state.value
        val kkm = here?.takeIf { seat.signedIn && it.kkmId == seat.kkm?.kkmId } ?: return null
        val written = kassa.ask { it.updateOfdToken(kkm.kkmId, seat.pin, token) }
        val reread = if (written is Answer.Done) kassa.ask { it.getKkm(kkm.kkmId) } else written.failure()
        return reread.also { if (it is Answer.Done) signIn.refresh(it.value) }
    }

    /** Отказ записи без значения — тем же отказом для ответа о кассе. */
    private fun Answer<Boolean>.failure(): Answer<KkmResponse> = when (this) {
        is Answer.Refused -> this
        is Answer.Failed -> this
        is Answer.Done -> Answer.Failed(UNWRITTEN)
    }

    private companion object {
        const val UNWRITTEN = "token not written"
    }
}
