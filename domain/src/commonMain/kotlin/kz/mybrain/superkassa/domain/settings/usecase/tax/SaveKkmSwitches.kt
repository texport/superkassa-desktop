package kz.mybrain.superkassa.domain.settings.usecase.tax

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Автозакрытие смены и автоизъятие наличных.
 *
 * Касса меняет их одним обращением, поэтому уходят оба каждый раз:
 * переключатель, который не тронули, уходит тем, что стоит у кассы.
 */
class SaveKkmSwitches(private val kassa: Kassa, private val signed: SignedKkm) {

    suspend operator fun invoke(autoClose: Boolean, autoCashout: Boolean): Answer<KkmResponse> =
        kassa.changeSeated(signed) { api, seat ->
            api.updateKkmSettings(seat.kkmId, seat.pin, autoClose, autoCashout)
        }
}
