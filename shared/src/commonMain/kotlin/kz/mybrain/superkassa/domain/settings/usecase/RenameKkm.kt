package kz.mybrain.superkassa.domain.settings.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Переименование выбранной кассы.
 *
 * Название уходит в кассу: так её зовут на входе, до того как кассир
 * набрал пин. Не принятое кассой остаётся хотя бы на этой машине — без
 * этого отказ оставлял бы кассу на входе одним регистрационным номером.
 * Своё название машины при удаче снимается: иначе оно перекрывало бы
 * только что записанное в кассу, и правка выглядела бы непринятой.
 */
class RenameKkm(private val kassa: Kassa, private val signed: SignedKkm, private val workplace: WorkplaceChoices) {

    /** @param name новое название; `null` — вернуть название от БФД. */
    suspend operator fun invoke(name: String?): Answer<KkmResponse> {
        val kkmId = signed.seat()?.kkmId
        val answer = kassa.changeSeated(signed) { api, seat -> api.updateKkmName(seat.kkmId, seat.pin, name) }
        kkmId?.let { workplace.rename(it, if (answer is Answer.Done) null else name) }
        return answer
    }
}
