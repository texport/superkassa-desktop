package kz.mybrain.superkassa.domain.journal.usecase

import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmState
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.journal.model.DocumentPages
import kz.mybrain.superkassa.domain.journal.model.Paged
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Сценарии журнала на кассе по заказу: какой вопрос уходит кассе и что
 * из ответа остаётся.
 */
class JournalUseCasesTest {
    private val core = FakeCore()
    private val signIn = SignIn()
    private val kassa = core.kassa()

    private fun enter(state: String = "ACTIVE") =
        signIn.enter(CoreScene.kkm(state = state), CoreScene.cashier(), CoreScene.PIN)

    @Test
    fun `без кассира кассу не спрашивают`(): Unit = runBlocking {
        assertIs<Answer.Failed>(ReadPeriodDocuments(kassa, signIn)(0L..1L, emptyList()))
        assertIs<Answer.Failed>(ReadShifts(kassa, signIn)(emptyList()))
        assertIs<Answer.Failed>(ReadQueue(kassa, signIn)())
        assertTrue(core.calls.isEmpty(), "касса спрошена без кассира: ${core.calls}")
    }

    /** Повтор на стыке страниц отбрасывается, а полная страница обещает следующую. */
    @Test
    fun `страница дописывается к прочитанному без повторов`(): Unit = runBlocking {
        val size = DocumentPages.DOCUMENTS
        core.on("listFiscalDocumentsByPeriod") { args ->
            val offset = args[4] as Int
            (offset until offset + size).map { CoreScene.document("d-${it.coerceAtLeast(1)}") }
        }
        enter()
        val shown = listOf(CoreScene.document("d-1"))

        val paged = assertIs<Answer.Done<Paged<*>>>(ReadPeriodDocuments(kassa, signIn)(0L..1L, shown)).value

        assertEquals(size, paged.items.size, "повтор первого документа не отброшен")
        assertTrue(paged.more)
    }

    /** Режим спрашивается у кассы в миг нажатия: прочитанное прежде могло устареть. */
    @Test
    fun `повтор очереди не просится вне режима программирования`(): Unit = runBlocking {
        val retry = RetryFailedQueue(kassa, signIn)
        var state = KkmState.ACTIVE.name
        core.on("getKkm") { CoreScene.kkm(state = state) }
        enter(state = KkmState.PROGRAMMING.name)
        state = KkmState.ACTIVE.name
        assertNull(retry(), "касса вышла из режима программирования, а повтор попрошен")
        assertFalse("getQueue" in core.calls, "очередь спрошена вне режима программирования")

        val queue = FakeCore().apply { on("retryFailed") { 2 } }
        core.on("getQueue") { queue.proxy(OfflineQueueApi::class.java) }
        state = KkmState.PROGRAMMING.name
        assertEquals(Answer.Done(2), retry())
    }

    /** Очередь читается вместе со свежим состоянием кассы, и шапка его видит. */
    @Test
    fun `очередь освежает кассу за кассой`(): Unit = runBlocking {
        val queue = FakeCore().apply { on("listQueue") { emptyList<Any>() } }
        core.on("getQueue") { queue.proxy(OfflineQueueApi::class.java) }
        core.on("getKkm") { CoreScene.kkm(state = KkmState.PROGRAMMING.name) }
        enter()

        ReadQueue(kassa, signIn)()

        assertEquals(KkmState.PROGRAMMING.name, signIn.state.value.kkm?.state)
        assertFalse(core.calls.indexOf("getKkm") > core.calls.indexOf("getQueue"), "касса освежена после очереди")
    }
}
