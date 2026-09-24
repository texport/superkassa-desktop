package kz.mybrain.superkassa.presentation.journal.queue

import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmState
import io.github.texport.superkassa.core.presentation.api.model.queue.QueueItemResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Очередь без окна: касса и её очередь по заказу.
 *
 * Очередь у ядра — отдельный фасад, и проверка подставляет его так же:
 * фасадом по заказу, отвечающим только на названное.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {
    private val queue = FakeCore()
    private val core = FakeCore().apply {
        on("getDocumentTypes") { emptyList<Any>() }
        on("getQueue") { queue.proxy(OfflineQueueApi::class.java) }
    }
    private val signIn = SignIn()
    private val notices = Notices()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(state: String = "ACTIVE"): QueueViewModel {
        core.on("getKkm") { CoreScene.kkm(state = state) }
        signIn.enter(CoreScene.kkm(state = state), CoreScene.cashier(), CoreScene.PIN)
        return queueModel(CoreScene.services(core, signIn, notices))
    }

    @Test
    fun `очередь прочитана, и ждущие отделены от отправленных`() {
        queue.on("listQueue") { listOf(task("q1", "PENDING"), task("q2", "FAILED"), task("q3", "SENT")) }
        val state = model().state.value

        assertTrue(state.read)
        assertEquals(listOf("q1", "q2"), state.waiting.map { it.id })
        assertEquals(listOf("q3"), state.sent.map { it.id })
        assertFalse(state.canRetry, "повтор обещан кассе не в режиме программирования")
    }

    @Test
    fun `отказ кассы — очередь не прочитана, а не пуста`() {
        queue.refuse("listQueue", "FORBIDDEN", ru = "Нет прав")
        val state = model().state.value

        assertFalse(state.read)
        assertEquals(Message.Refusal("Нет прав", "FORBIDDEN"), notices.last)
    }

    @Test
    fun `сбой кассы — очередь не прочитана`() {
        queue.on("listQueue") { error("database is locked") }
        val state = model().state.value

        assertFalse(state.read)
        assertIs<Message.Failed>(notices.last)
    }

    @Test
    fun `повтор в режиме программирования возвращает неудачные в очередь`() {
        var retried = false
        queue.on("listQueue") { listOf(task("q2", if (retried) "PENDING" else "FAILED")) }
        queue.on("retryFailed") { 1.also { retried = true } }
        val model = model(state = KkmState.PROGRAMMING.name)
        assertTrue(model.state.value.canRetry)

        model.retryFailed()

        assertTrue(retried)
        assertEquals(Message.Done(textsOf(Language.Ru).common.queue.retryDone), notices.last)
        assertTrue(model.state.value.failed.isEmpty(), "очередь не перечитана после повтора")
    }

    private fun task(id: String, status: String) = QueueItemResponse(
        id = id, lane = "OFFLINE", type = "TICKET", status = status, attempt = 1,
        nextAttemptAt = null, lastError = null, errorRu = null, errorKk = null, errorEn = null
    )
}
