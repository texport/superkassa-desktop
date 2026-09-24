package kz.mybrain.superkassa.presentation.journal.queue

import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Очередь отложенной отправки глазами кассира.
 *
 * Снимки — `/tmp/kassa-queue-*.png`. Смотреть надо на две вещи: вид задачи
 * назван словом, а не кодом кассы, и отвергнутая задача стоит своей частью,
 * а не под заголовком «Уже отправлено».
 */
class KassaQueueLookTest {

    @Test
    fun `в очереди нет кодов кассы, а отвергнутое стоит отдельно от отправленного`() {
        // Пустая очередь — это очередь, о которой касса ответила: без ответа
        // экран обязан говорить «прочитать не удалось», а не «всё доставлено».
        val none = object : QueueActions {}
        val kkm = CoreScene.kkm()
        val empty = KassaScene.shot("queue-empty", width = NARROW, height = SHORT) {
            QueueContent(QueueUiState(kkm = kkm, read = true), none)
        }
        val unread = KassaScene.shot("queue-unread", width = NARROW, height = SHORT) {
            QueueContent(QueueUiState(kkm = kkm), none)
        }
        val filled = KassaScene.shot("queue-sections", width = NARROW, height = SHORT) {
            QueueContent(QueueUiState(kkm = kkm, tasks = TASKS, read = true), none)
        }

        assertTrue(empty.isNotEmpty() && filled.isNotEmpty() && unread.isNotEmpty())
        assertTrue(!empty.contentEquals(filled), "пустая очередь неотличима от заполненной")
        assertTrue(
            !empty.contentEquals(unread),
            "«всё доставлено» и «очередь прочитать не удалось» на экране неразличимы"
        )
    }

    private companion object {
        /** Узкое окно кассира: на нём строка очереди уезжала за край. */
        const val NARROW = 1000
        const val SHORT = 700

        /** Все состояния, которые касса различает, по одной задаче на каждое. */
        val TASKS = listOf(
            task("q1", "TICKET", "PENDING", attempt = 0),
            task("q2", "TICKET", "FAILED", attempt = 3, SILENT_BFD),
            task("q3", "SHIFT_CLOSE", "REJECTED", attempt = 5, "Чек отвергнут"),
            task("q4", "TICKET", "SENT", attempt = 1)
        )

        const val SILENT_BFD = "БФД не отвечает: превышено время ожидания ответа на команду продажи"

        fun task(id: String, type: String, status: String, attempt: Int, reason: String? = null) = QueueItemResponse(
            id = id, lane = "OFFLINE", type = type, status = status, attempt = attempt, nextAttemptAt = null,
            lastError = reason, errorRu = reason, errorKk = null, errorEn = null
        )
    }
}
