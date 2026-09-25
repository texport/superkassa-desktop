package kz.mybrain.superkassa.data.eds

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.egovmobile.EgovDocument
import kz.mybrain.superkassa.integrations.egovmobile.EgovMobile
import kz.mybrain.superkassa.integrations.egovmobile.EgovSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class EgovSignerTest {
    private val desk = SignDesk()
    private val signed = CompletableDeferred<String>()
    private val document = EgovDocument("Superkassa", "Подпись", "Қолтаңба", "Signature")

    @Test
    fun qrAndLaunchLinkAreShownUntilTheSignatureArrives() = runTest {
        val signer = EgovSigner(relay(), desk) { document }
        val signature = async(start = CoroutineStart.UNDISPATCHED) { signer.sign("AA==") }
        val shown = awaitRequest()
        assertEquals("https://launch.egov/1", shown.launch)
        signed.complete(
            """{"documentsToSign":[{"id":1,"nameRu":"","nameKz":"","nameEn":"",""" +
                """"document":{"file":{"data":"MIIC"}}}]}"""
        )
        assertEquals("MIIC", signature.await())
        assertNull(desk.request.value)
    }

    @Test
    fun ownerCancelAtTheCashRegisterIsCancelledSigning() = runTest {
        val signer = EgovSigner(relay(), desk) { document }
        val signature = async(start = CoroutineStart.UNDISPATCHED) { runCatching { signer.sign("AA==") } }
        awaitRequest()
        desk.answer(SignAnswer.Cancel)
        val refusal = assertFailsWith<EdsRefusal> { signature.await().getOrThrow() }
        assertEquals(Signer.CANCELLED, refusal.detail)
    }

    @Test
    fun cancelInEgovMobileIsCancelledSigning() = runTest {
        signed.complete("""{"status":"CANCELED","documentsToSign":[]}""")
        val refusal = assertFailsWith<EdsRefusal> { EgovSigner(relay(), desk) { document }.sign("AA==") }
        assertEquals(Signer.CANCELLED, refusal.detail)
    }

    private suspend fun awaitRequest(): SignRequest.EgovMobile {
        while (desk.request.value == null) kotlinx.coroutines.yield()
        return assertIs<SignRequest.EgovMobile>(desk.request.value)
    }

    private fun relay() = EgovMobile(
        settings = EgovSettings(relay = "https://relay.test"),
        engine = MockEngine { request ->
            val body = when {
                request.url.encodedPath == "/api/egovQr" -> REGISTERED
                request.method == HttpMethod.Post -> """{"signURL":"https://relay.test/api/egovQr/7"}"""
                else -> signed.await()
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    )

    private companion object {
        const val REGISTERED = """{"qrCode":"iVBORw==","eGovMobileLaunchLink":"https://launch.egov/1",""" +
            """"dataURL":"https://relay.test/api/egovQr/7","signURL":"https://relay.test/api/egovQr/7"}"""
    }
}
