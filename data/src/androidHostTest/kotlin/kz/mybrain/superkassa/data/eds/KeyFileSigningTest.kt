package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.model.signature.KeyProblem
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KeyFileSigningTest {
    private val desk = SignDesk()
    private val picked = ArrayDeque(
        listOf(KeyFile("RSA256_first.p12", byteArrayOf(1)), KeyFile("GOST512_second.p12", byteArrayOf(2)))
    )
    private val signing = KeyFileSigning({ picked.removeFirstOrNull() }, desk)

    @Test
    fun closedFilePickerCancelsSigning() = runTest {
        picked.clear()
        val refusal = assertFailsWith<EdsRefusal> { signing.sign("AA==") }
        assertEquals(Signer.CANCELLED, refusal.detail)
    }

    @Test
    fun unreadableFileKeepsTheWindowOpenAndErasesThePassword() = runTest {
        val signature = async(start = CoroutineStart.UNDISPATCHED) { runCatching { signing.sign("AA==") } }
        assertEquals(SignRequest.KeyPassword("RSA256_first.p12"), asked())
        val password = "Qwerty12".toCharArray()
        desk.answer(SignAnswer.Password(password))
        assertEquals(SignRequest.KeyPassword("RSA256_first.p12", KeyProblem.Unreadable), askedAgain())
        assertTrue(password.all { it == '\u0000' })
        desk.answer(SignAnswer.Cancel)
        assertEquals(Signer.CANCELLED, assertFailsWith<EdsRefusal> { signature.await().getOrThrow() }.detail)
    }

    @Test
    fun anotherFileReplacesTheChosenOne() = runTest {
        val signature = async(start = CoroutineStart.UNDISPATCHED) { runCatching { signing.sign("AA==") } }
        asked()
        desk.answer(SignAnswer.OtherFile)
        assertEquals(SignRequest.KeyPassword("GOST512_second.p12"), askedAgain())
        desk.answer(SignAnswer.Cancel)
        signature.await()
    }

    private var last: SignRequest? = null

    private suspend fun asked(): SignRequest {
        while (desk.request.value == null) yield()
        return checkNotNull(desk.request.value).also { last = it }
    }

    private suspend fun askedAgain(): SignRequest {
        while (desk.request.value == null || desk.request.value === last) yield()
        return checkNotNull(desk.request.value).also { last = it }
    }
}
