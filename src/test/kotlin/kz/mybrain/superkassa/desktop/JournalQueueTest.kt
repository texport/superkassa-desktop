package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.queue.QueueState
import kz.mybrain.superkassa.desktop.ui.queue.failedTasks
import kz.mybrain.superkassa.desktop.ui.queue.queueState
import kz.mybrain.superkassa.desktop.ui.queue.queueStateOf
import kz.mybrain.superkassa.desktop.ui.queue.rejectedTasks
import kz.mybrain.superkassa.desktop.ui.queue.sentTasks
import kz.mybrain.superkassa.desktop.ui.queue.waitingTasks
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
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
    fun `отвергнутое не считается отправленным`() {
        // Отвергнутая задача стояла под заголовком «Уже отправлено»
        // с плашкой «Не будет отправлен»: заголовок спорил со строкой
        // под ним, а счёт отправленных включал то, что не ушло.
        val tasks = listOf(task("PENDING"), task("SENT"), task("REJECTED"))

        assertEquals(listOf("SENT"), sentTasks(tasks).map { it.status })
        assertEquals(listOf("REJECTED"), rejectedTasks(tasks).map { it.status })
        assertTrue(waitingTasks(tasks).none { it.status == "REJECTED" })
    }

    /**
     * Пустая очередь и очередь, о которой узел не ответил, — разные вещи.
     *
     * «Ждущих документов нет — всё доставлено» владелец читает как порядок
     * и уходит с экрана. Говорить это можно только вслед за ответом узла.
     */
    @Test
    fun `непрочитанная очередь не выдаётся за пустую`() {
        val texts = stringsOf(Language.Ru)
        val journal = journalTexts(Language.Ru).queue

        val unread = queueState(texts, journal, tasks = 0, busy = false, read = false, blocked = false) {}
        val empty = queueState(texts, journal, tasks = 0, busy = false, read = true, blocked = false) {}

        assertEquals(
            ScreenState.Trouble(journal.unread, journal.unreadHint),
            (unread as ScreenState.Trouble).copy(onRetry = null)
        )
        assertTrue(empty is ScreenState.Empty && empty.hint == journal.emptyHint)
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
