package kz.mybrain.superkassa.domain.settings.usecase.ofd

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm

/**
 * Сверка кассы с БФД: сведения о кассе или её счётчики.
 *
 * Всё, что касса знает о себе, приходит от БФД; разойтись они могут после
 * автономной работы или замены сведений в кабинете. Сверка меняет кассу,
 * а ответом её не отдаёт — касса перечитывается.
 */
class SyncWithBfd(private val kassa: Kassa, private val signed: SignedKkm) {

    /** Что сверять: сведения о кассе — организацию, адрес, номера — или счётчики и номер смены. */
    enum class Part { Service, Counters }

    suspend operator fun invoke(part: Part): Answer<OfdCommandResponse> {
        val answer = kassa.askSeated(signed) { api, seat ->
            when (part) {
                Part.Service -> api.syncOfdServiceInfo(seat.kkmId, seat.pin)
                Part.Counters -> api.syncOfdCounters(seat.kkmId, seat.pin)
            }
        }
        RefreshKkm(kassa, signed)()
        return answer
    }
}
