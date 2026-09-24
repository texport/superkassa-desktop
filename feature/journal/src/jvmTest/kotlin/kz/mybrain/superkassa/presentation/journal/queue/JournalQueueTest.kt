package kz.mybrain.superkassa.presentation.journal.queue

import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.journal.model.QueueState
import kz.mybrain.superkassa.domain.journal.model.failedTasks
import kz.mybrain.superkassa.domain.journal.model.queueStateOf
import kz.mybrain.superkassa.domain.journal.model.reason
import kz.mybrain.superkassa.domain.journal.model.rejectedTasks
import kz.mybrain.superkassa.domain.journal.model.sentTasks
import kz.mybrain.superkassa.domain.journal.model.waitingTasks
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private fun task(status: String) = QueueItemResponse(
        id = "q-$status", lane = "OFFLINE", type = "TICKET", status = status, attempt = 0,
        nextAttemptAt = null, lastError = null, errorRu = null, errorKk = null, errorEn = null
    )

    @Test
    fun `касса различает пять состояний, и все они разобраны`() {
        assertEquals(QueueState.Queued, queueStateOf("PENDING"))
        assertEquals(QueueState.Sending, queueStateOf("IN_PROGRESS"))
        assertEquals(QueueState.Failed, queueStateOf("FAILED"))
        assertEquals(QueueState.Sent, queueStateOf("SENT"))
        assertEquals(QueueState.Rejected, queueStateOf("REJECTED"))
        assertEquals(QueueState.Unknown, queueStateOf("ЧТО-ТО НОВОЕ"))
        assertEquals(QueueState.Unknown, queueStateOf(null))
    }

    @Test
    fun `задача в работе ждёт отправки, а не считается доставленной`() {
        val tasks = listOf(task("PENDING"), task("IN_PROGRESS"), task("FAILED"), task("SENT"))

        assertEquals(
            listOf("PENDING", "IN_PROGRESS", "FAILED"),
            waitingTasks(tasks).map { it.status },
            "глубина очереди — это всё, что ещё не в БФД"
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
     * Пустая очередь и очередь, о которой касса не ответила, — разные вещи.
     *
     * «Ждущих документов нет — всё доставлено» владелец читает как порядок
     * и уходит с экрана. Говорить это можно только вслед за ответом кассы.
     */
    @Test
    fun `непрочитанная очередь не выдаётся за пустую`() {
        val texts = textsOf(Language.Ru).common
        val journal = textsOf(Language.Ru).journal.queue

        val none = object : QueueActions {}
        val unread = queueState(texts, journal, QueueUiState(read = false), none)
        val empty = queueState(texts, journal, QueueUiState(read = true), none)

        assertEquals(
            ScreenState.Trouble(journal.unread, journal.unreadHint),
            (unread as ScreenState.Trouble).copy(onRetry = null)
        )
        assertTrue(empty is ScreenState.Empty && empty.hint == journal.emptyHint)
    }

    @Test
    fun `у непрочитанной очереди нет и числа ждущих`() {
        assertEquals("3", waitingText(read = true, waiting = 3))
        assertEquals("0", waitingText(read = true, waiting = 0))
        assertEquals(
            Glyphs.DASH,
            waitingText(read = false, waiting = 0),
            "крупный ноль над непрочитанной очередью читается как порядок"
        )
    }

    @Test
    fun `повторяются только неудачные`() {
        val tasks = listOf(task("PENDING"), task("FAILED"), task("SENT"), task("IN_PROGRESS"))

        assertEquals(
            listOf("FAILED"),
            failedTasks(tasks).map { it.status },
            "кнопка повтора обещает ровно то, что делает касса"
        )
    }

    /** Причина — на языке кассира; нет перевода — исходная запись, а не пустота. */
    @Test
    fun `причина неудачи на языке кассира`() {
        val failed = task("FAILED").copy(lastError = "timeout", errorRu = "Нет ответа", errorKk = "Жауап жоқ")

        assertEquals("Жауап жоқ", failed.reason("kk"))
        assertEquals("Нет ответа", failed.reason("en"), "без перевода причина пропала")
        assertEquals("timeout", task("FAILED").copy(lastError = "timeout").reason("ru"))
        assertEquals(null, task("FAILED").reason("ru"))
    }
}
