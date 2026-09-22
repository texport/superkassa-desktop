package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.adoptQueue
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.ui.queue.QueueScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Очередь отложенной отправки глазами кассира.
 *
 * Снимки — `/tmp/kassa-queue-*.png`. Смотреть надо на две вещи: вид задачи
 * назван словом, а не кодом узла, и отвергнутая задача стоит своей частью,
 * а не под заголовком «Уже отправлено».
 */
class KassaQueueLookTest {

    @Test
    fun `в очереди нет кодов узла, а отвергнутое стоит отдельно от отправленного`() {
        val empty = KassaScene.shot("queue-empty", width = NARROW, height = SHORT) {
            QueueScreen(KassaScene.session("queue-empty", shift = KassaScene.openShift()))
        }
        val filled = KassaScene.shot("queue-sections", width = NARROW, height = SHORT) {
            val session = KassaScene.session("queue-filled", shift = KassaScene.openShift())
            session.adoptQueue(TASKS)
            QueueScreen(session)
        }

        assertTrue(empty.isNotEmpty() && filled.isNotEmpty())
        assertTrue(!empty.contentEquals(filled), "пустая очередь неотличима от заполненной")
    }

    private companion object {
        /** Узкое окно кассира: на нём строка очереди уезжала за край. */
        const val NARROW = 1000
        const val SHORT = 700

        /** Все состояния, которые узел различает, по одной задаче на каждое. */
        val TASKS = listOf(
            QueueTask(id = "q1", type = "TICKET", status = "PENDING", attempt = 0),
            QueueTask(
                id = "q2",
                type = "TICKET",
                status = "FAILED",
                attempt = 3,
                errorRu = "БФД не отвечает: превышено время ожидания ответа на команду продажи"
            ),
            QueueTask(id = "q3", type = "SHIFT_CLOSE", status = "REJECTED", attempt = 5, errorRu = "Чек отвергнут"),
            QueueTask(id = "q4", type = "TICKET", status = "SENT", attempt = 1)
        )
    }
}
