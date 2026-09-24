package kz.mybrain.superkassa.domain.journal.usecase

import kz.mybrain.superkassa.domain.journal.model.readyToRetryQueue
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Возвращает неудавшиеся задачи в очередь.
 *
 * Касса делает это только в режиме программирования и при закрытой смене;
 * иначе повтор не просится вовсе — отказ с протокольным «ККМ должна быть в режиме
 * PROGRAMMING» кассиру ничего не объясняет. Режим спрашивается у самой
 * кассы в миг нажатия: состояние, прочитанное минуту назад, могло
 * устареть, а перечитанную кассу видят все разделы.
 */
class RetryFailedQueue(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return сколько задач возвращено; `null` — касса не в режиме программирования или смена открыта. */
    suspend operator fun invoke(): Answer<Int>? {
        val kkm = kassa.changeSeated(signed) { api, seat -> api.getKkm(seat.kkmId) }
        return when (kkm) {
            is Answer.Refused -> kkm
            is Answer.Failed -> kkm
            is Answer.Done -> if (kkm.value.readyToRetryQueue) retry() else null
        }
    }

    private suspend fun retry(): Answer<Int> =
        kassa.askSeated(signed) { api, seat -> api.queue.retryFailed(seat.kkmId, seat.pin) }
}
