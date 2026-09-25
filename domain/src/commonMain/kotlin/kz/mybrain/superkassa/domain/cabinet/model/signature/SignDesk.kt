package kz.mybrain.superkassa.domain.cabinet.model.signature

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.selects.select
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.Signer

/**
 * Стол подписи: здесь подписывающий спрашивает владельца посреди подписи.
 *
 * NCALayer показывает своё окно сам, а у eGov mobile и файла ключа окна
 * в кассе нет. Подписывающий кладёт на стол просьбу — QR или вопрос
 * о пароле, — экран её показывает и возвращает ответ владельца. Вход,
 * заявление и мастер зовут только [Signer.sign] и о столе не знают:
 * просьбу показывает одно окно на всё приложение.
 *
 * Просьба снимается со стола в любом исходе подписи — удачей, отказом
 * или отменой, — иначе окно осталось бы висеть над подписью, которой
 * уже никто не ждёт.
 */
class SignDesk {
    private val current = MutableStateFlow<SignRequest?>(null)
    private val pending = MutableStateFlow<CompletableDeferred<SignAnswer>?>(null)

    /** Что показать владельцу сейчас; `null` — подписывающий ничего не просит. */
    val request: StateFlow<SignRequest?> = current.asStateFlow()

    /** Спрашивает владельца и ждёт его ответа. */
    suspend fun ask(request: SignRequest): SignAnswer = asking(request) { it.await() }

    /**
     * Показывает просьбу, пока идёт [work]: подпись приходит не через стол,
     * а своим путём. Отмена владельцем прерывает [work] отказом подписи.
     *
     * @throws EdsRefusal владелец отменил подпись.
     */
    suspend fun <T> showing(request: SignRequest, work: suspend () -> T): T = asking(request) { answer ->
        coroutineScope {
            val done = async { work() }
            select {
                done.onAwait { it }
                answer.onAwait {
                    done.cancel()
                    throw EdsRefusal(EdsProblem.Declined, Signer.CANCELLED)
                }
            }
        }
    }

    /** Ответ владельца на просьбу; просьбы нет — ответ некому отдать, и он пропадает. */
    fun answer(reply: SignAnswer) {
        pending.value?.complete(reply)
    }

    private suspend fun <T> asking(request: SignRequest, block: suspend (Deferred<SignAnswer>) -> T): T {
        val answer = CompletableDeferred<SignAnswer>()
        pending.value = answer
        current.value = request
        try {
            return block(answer)
        } finally {
            if (pending.compareAndSet(answer, null)) current.value = null
        }
    }
}
