package kz.mybrain.superkassa.domain.cabinet.model.signature

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SignDeskTest {
    private val asked = SignRequest.KeyPassword("GOST512_owner.p12")

    @Test
    fun answerReachesTheAskingSignerAndClearsTheRequest() = runTest {
        val desk = SignDesk()
        val answer = async(start = CoroutineStart.UNDISPATCHED) { desk.ask(asked) }
        assertEquals(asked, desk.request.value)
        desk.answer(SignAnswer.OtherFile)
        assertSame(SignAnswer.OtherFile, answer.await())
        assertNull(desk.request.value)
    }

    @Test
    fun cancelledSigningLeavesNoRequestBehind() = runTest {
        val desk = SignDesk()
        val answer = async(start = CoroutineStart.UNDISPATCHED) { desk.ask(asked) }
        answer.cancelAndJoin()
        assertNull(desk.request.value)
    }

    @Test
    fun shownRequestLastsWhileTheWorkRuns() = runTest {
        val desk = SignDesk()
        val signature = CompletableDeferred<String>()
        val shown = SignRequest.EgovMobile("https://launch", byteArrayOf(1), until = null)
        val signed = async(start = CoroutineStart.UNDISPATCHED) { desk.showing(shown) { signature.await() } }
        assertSame(shown, desk.request.value)
        signature.complete("MIAG")
        assertEquals("MIAG", signed.await())
        assertNull(desk.request.value)
    }

    @Test
    fun ownerCancelStopsTheWorkWithCancelledRefusal() = runTest {
        val desk = SignDesk()
        val never = CompletableDeferred<String>()
        val shown = SignRequest.EgovMobile("l", byteArrayOf(), until = null)
        val signed = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { desk.showing(shown) { never.await() } }
        }
        desk.answer(SignAnswer.Cancel)
        val refusal = assertFailsWith<EdsRefusal> { signed.await().getOrThrow() }
        assertEquals(Signer.CANCELLED, refusal.detail)
        assertTrue(refusal.cancelled)
        assertTrue(never.isCancelled || !never.isCompleted)
        assertNull(desk.request.value)
    }
}
