package kz.mybrain.superkassa.presentation.journal.queue

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.journal.model.canRetryQueue
import kz.mybrain.superkassa.domain.journal.model.failedTasks
import kz.mybrain.superkassa.domain.journal.model.rejectedTasks
import kz.mybrain.superkassa.domain.journal.model.sentTasks
import kz.mybrain.superkassa.domain.journal.model.waitingTasks
import kz.mybrain.superkassa.domain.kkm.model.isBlocked
import kz.mybrain.superkassa.domain.kkm.model.isProgramming

/**
 * Очередь отложенной отправки, какой её видит кассир.
 *
 * @property tasks задачи, как их отдала касса.
 * @property read ответила ли касса об очереди: «ждущих нет» — утверждение
 *   о кассе, и делать его можно только вслед за ответом.
 * @property reading ответа ещё нет.
 * @property documentTypes названия видов документа со слов кассы: в строке
 *   задачи стоял код — «Задача: TICKET».
 */
data class QueueUiState(
    val kkm: KkmResponse? = null,
    val tasks: List<QueueItemResponse> = emptyList(),
    val read: Boolean = false,
    val reading: Boolean = false,
    val retrying: Boolean = false,
    val documentTypes: Map<String, TrilingualMessageResponse> = emptyMap()
) {
    val waiting: List<QueueItemResponse> get() = waitingTasks(tasks)
    val failed: List<QueueItemResponse> get() = failedTasks(tasks)
    val rejected: List<QueueItemResponse> get() = rejectedTasks(tasks)
    val sent: List<QueueItemResponse> get() = sentTasks(tasks)

    /** Повтор касса принимает только в режиме программирования. */
    val programming: Boolean get() = kkm?.isProgramming == true

    /** Пустая очередь заблокированной кассы — не порядок: пробить больше нечего. */
    val blocked: Boolean get() = kkm?.isBlocked == true

    /** Есть что повторять, касса готова повторять, и повтор уже не идёт. */
    val canRetry: Boolean get() = canRetryQueue(kkm, tasks) && !retrying
}
