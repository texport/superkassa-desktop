package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.ui.queue.QueueState
import kz.mybrain.superkassa.desktop.ui.queue.failedTasks
import kz.mybrain.superkassa.desktop.ui.queue.queueStateOf
import kz.mybrain.superkassa.desktop.ui.queue.waitingTasks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Очередь: что считается ждущим и что повторяется.
 *
 * Назвать доставленным недоставленное — потерянный документ, поэтому
 * неизвестное состояние считается ждущим, а не отправленным.
 */
class JournalQueueTest {

    private fun task(status: String?) = QueueTask(id = "q-$status", type = "TICKET", status = status)

    @Test
    fun `узел различает пять состояний, и все они разобраны`() {
        assertEquals(QueueState.Queued, queueStateOf("PENDING"))
        assertEquals(QueueState.Sending, queueStateOf("IN_PROGRESS"))
        assertEquals(QueueState.Failed, queueStateOf("FAILED"))
        assertEquals(QueueState.Sent, queueStateOf("SENT"))
        assertEquals(QueueState.Unknown, queueStateOf("ЧТО-ТО НОВОЕ"))
        assertEquals(QueueState.Unknown, queueStateOf(null))
    }

    @Test
    fun `задача в работе ждёт отправки, а не считается доставленной`() {
        val tasks = listOf(task("PENDING"), task("IN_PROGRESS"), task("FAILED"), task("SENT"))

        assertEquals(
            listOf("PENDING", "IN_PROGRESS", "FAILED"),
            waitingTasks(tasks).map { it.status },
            "глубина очереди — это всё, что ещё не в ОФД"
        )
    }

    @Test
    fun `неизвестное состояние считается ждущим`() {
        assertTrue(waitingTasks(listOf(task("PAUSED"))).size == 1)
        assertTrue(queueStateOf("PAUSED").isWaiting)
    }

    @Test
    fun `повторяются только неудачные`() {
        val tasks = listOf(task("PENDING"), task("FAILED"), task("SENT"), task("IN_PROGRESS"))

        assertEquals(
            listOf("FAILED"),
            failedTasks(tasks).map { it.status },
            "кнопка повтора обещает ровно то, что делает узел"
        )
    }
}
