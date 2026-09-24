package kz.mybrain.superkassa.domain.journal.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import kz.mybrain.superkassa.domain.kkm.model.isProgramming

/**
 * Состояние задачи очереди с точки зрения кассира.
 *
 * Касса различает несколько состояний, и одно из них — «отправляется
 * прямо сейчас» — приложение раньше не знало вовсе: такая задача попадала
 * в отправленные и показывалась кассиру голым кодом.
 */
enum class QueueState {
    /** Ждёт своей очереди. */
    Queued,

    /** Обработчик уже взял задачу. */
    Sending,

    /** Попытка не удалась; повтор возможен. */
    Failed,

    /** Доставлена в БФД. */
    Sent,

    /**
     * Отвергнута окончательно: повтора не будет.
     *
     * Отличается от [Failed] тем, что кнопка «Повторить неудачные» её
     * не берёт: БФД отказала по существу либо запрос не удалось собрать.
     * Разбирать такую задачу человеку.
     */
    Rejected,

    /** Состояние, которого приложение не знает. */
    Unknown;

    /**
     * Ждёт ли задача отправки.
     *
     * Неизвестное состояние считается ждущим намеренно: назвать
     * недоставленным то, что доставлено, — мелкая неточность, а назвать
     * доставленным недоставленное — потерянный документ.
     */
    val isWaiting: Boolean get() = this != Sent && this != Rejected
}

/**
 * Коды состояний задачи, которыми касса отмечает очередь.
 *
 * Разбираются здесь, один раз: строкой в коде экрана код расходился бы
 * со справочником при первой же правке.
 */
object QueueCodes {
    val queued: Set<String> = setOf("PENDING")
    val sending: Set<String> = setOf("IN_PROGRESS", "PROCESSING")
    val failed: Set<String> = setOf("FAILED")
    val sent: Set<String> = setOf("SENT", "SUCCESS")
    val rejected: Set<String> = setOf("REJECTED")
}

/** Состояние задачи по коду кассы. */
fun queueStateOf(status: String?): QueueState = when (status) {
    in QueueCodes.queued -> QueueState.Queued
    in QueueCodes.sending -> QueueState.Sending
    in QueueCodes.failed -> QueueState.Failed
    in QueueCodes.sent -> QueueState.Sent
    in QueueCodes.rejected -> QueueState.Rejected
    else -> QueueState.Unknown
}

/** Состояние задачи. */
val QueueItemResponse.state: QueueState get() = queueStateOf(status)

/** Задачи, которые ещё не дошли до БФД. */
fun waitingTasks(tasks: List<QueueItemResponse>): List<QueueItemResponse> = tasks.filter { it.state.isWaiting }

/** Задачи, которые повторяет кнопка «Повторить неудачные». */
fun failedTasks(tasks: List<QueueItemResponse>): List<QueueItemResponse> =
    tasks.filter { it.state == QueueState.Failed }

/**
 * Можно ли просить кассу повторить неудачные.
 *
 * Повтор касса принимает только в режиме программирования и при закрытой
 * смене, а без неудачных задач повторять нечего: кнопка, которая обещает
 * то, чего касса не сделает, хуже отсутствующей. В режим программирования
 * касса пускает и при открытой смене — и тогда отказывает в повторе.
 */
fun canRetryQueue(kkm: KkmResponse?, tasks: List<QueueItemResponse>): Boolean =
    kkm?.readyToRetryQueue == true && failedTasks(tasks).isNotEmpty()

/** Касса примет повтор очереди: режим программирования, смена закрыта. */
val KkmResponse.readyToRetryQueue: Boolean
    get() = isProgramming && !isShiftOpen

/** Задачи, отвергнутые окончательно: повтор их не берёт, а на экране они видны. */
fun rejectedTasks(tasks: List<QueueItemResponse>): List<QueueItemResponse> =
    tasks.filter { it.state == QueueState.Rejected }

/**
 * Задачи, которые касса уже отправила.
 *
 * Отвергнутые сюда не попадают, хотя ждать их тоже нечего: они не ушли
 * и не уйдут, и в счёте отправленных их быть не должно.
 */
fun sentTasks(tasks: List<QueueItemResponse>): List<QueueItemResponse> = tasks.filter { it.state == QueueState.Sent }

/**
 * Причина последней неудачи на языке кассира.
 *
 * Касса хранит её на трёх языках; нет перевода — остаётся исходная
 * запись, по которой обслуживание всё равно отличит «БФД не отвечает»
 * от «чек отвергнут».
 *
 * @param language код языка кассира: `ru`, `kk` или `en`.
 */
fun QueueItemResponse.reason(language: String): String? {
    val words = when (language) {
        "kk" -> errorKk
        "en" -> errorEn
        else -> errorRu
    }
    return (words ?: errorRu ?: lastError)?.takeIf { it.isNotBlank() }
}
