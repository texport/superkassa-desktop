package kz.mybrain.superkassa.domain.settings.usecase.receipt

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Печатная форма чека кассы: язык, ширина ленты, реклама и свои строки.
 *
 * Касса меняет оформление целиком: правка одного поля несёт все прочие
 * как есть, иначе касса вернула бы их к умолчаниям.
 */
class SaveReceiptForm(private val kassa: Kassa, private val signed: SignedKkm) {

    suspend operator fun invoke(branding: ReceiptBrandingRequest): Answer<KkmResponse> =
        kassa.changeSeated(signed) { api, seat -> api.updateBrandingSettings(seat.kkmId, seat.pin, branding) }
}
