package kz.mybrain.superkassa.domain.settings.usecase.ofd

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.signin.usecase.RefreshKkm

/**
 * Замена токена ОФД.
 *
 * Токен выдаёт ОФД и он же его отзывает: по коду «неверный токен» касса
 * встаёт и не выходит из блокировки, пока не введён новый. Токен уходит
 * в кассу и нигде в приложении не хранится.
 */
class ReplaceOfdToken(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @param token новый токен: одни цифры. */
    suspend operator fun invoke(token: String): Answer<Boolean> {
        val answer = kassa.askSeated(signed) { api, seat -> api.updateOfdToken(seat.kkmId, seat.pin, token) }
        if (answer is Answer.Done) RefreshKkm(kassa, signed)()
        return answer
    }
}
