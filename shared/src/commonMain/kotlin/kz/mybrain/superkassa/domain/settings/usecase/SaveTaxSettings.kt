package kz.mybrain.superkassa.domain.settings.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Налоговый режим кассы и её ставка по умолчанию — одним обращением.
 *
 * Касса принимает их только в режиме программирования, при закрытой
 * смене и пустой очереди: сменить режим посреди смены значит получить
 * в одном Z-отчёте чеки с разными налогами.
 */
class SaveTaxSettings(private val kassa: Kassa, private val signed: SignedKkm) {

    suspend operator fun invoke(regime: TaxRegime, group: VatGroup): Answer<KkmResponse> =
        kassa.changeSeated(signed) { api, seat -> api.updateTaxSettings(seat.kkmId, seat.pin, regime, group) }
}
