package kz.mybrain.superkassa.domain.settings.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.model.askSeated

/**
 * Снятие кассы с этого рабочего места.
 *
 * Касса удаляется вместе с документами, а рабочее место уходит на выбор
 * кассы: оставаться в настройках удалённой нельзя — каждое следующее
 * обращение отвечало бы «касса не найдена».
 */
class DecommissionKkm(private val kassa: Kassa, private val signIn: SignIn) {

    suspend operator fun invoke(): Answer<Boolean> {
        val answer = kassa.askSeated(signIn) { api, seat -> api.deleteKkm(seat.kkmId, seat.pin) }
        if (answer is Answer.Done) signIn.switchKkm()
        return answer
    }
}
