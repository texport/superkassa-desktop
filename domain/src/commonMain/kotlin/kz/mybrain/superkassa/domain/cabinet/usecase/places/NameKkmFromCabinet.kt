package kz.mybrain.superkassa.domain.cabinet.usecase.places

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn

/**
 * Даёт кассе этой машины название, которое ей дал владелец в кабинете.
 *
 * Название нужно кассиру на входе, когда кабинет закрыт. Пишется оно
 * в кассу, за которой сейчас работают, — её пином; уже названную кассу
 * кабинет не переименовывает. Касса узнаётся по идентификатору у ОФД.
 *
 * @return ответ кассы; `null` — называть нечего.
 */
class NameKkmFromCabinet(private val kassa: Kassa, private val signIn: SignIn) {
    suspend operator fun invoke(registers: List<CabinetRegister>): Answer<KkmResponse>? {
        val seat = signIn.state.value
        val kkm = seat.kkm?.takeIf { seat.signedIn && it.name.isNullOrBlank() }
        val name = kkm?.let { unnamed ->
            registers.firstOrNull { it.kkmId.toString() == unnamed.ofdSystemId }?.internalName
        }
        if (kkm == null || name.isNullOrBlank()) return null
        return kassa.ask { it.updateKkmName(kkm.kkmId, seat.pin, name) }.also { answer ->
            if (answer is Answer.Done) signIn.refresh(answer.value)
        }
    }
}
