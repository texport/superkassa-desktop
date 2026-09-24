package kz.mybrain.superkassa.domain.journal.usecase

import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.model.changeSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Очередь отложенной отправки вместе со свежим состоянием кассы.
 *
 * Касса перечитывается раньше очереди: повтор неудачных она принимает
 * только в режиме программирования, и кнопка без свежего состояния
 * обещала бы то, чего касса не сделает. Касса, не ответившая о себе,
 * не мешает прочитать очередь: прежнее её состояние остаётся.
 */
class ReadQueue(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return задачи очереди. */
    suspend operator fun invoke(): Answer<List<QueueItemResponse>> {
        kassa.changeSeated(signed) { api, seat -> api.getKkm(seat.kkmId) }
        return kassa.askSeated(signed) { api, seat -> api.queue.listQueue(seat.kkmId, seat.pin) }
    }
}
