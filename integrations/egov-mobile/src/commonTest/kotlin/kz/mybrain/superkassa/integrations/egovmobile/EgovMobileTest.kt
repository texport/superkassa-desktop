package kz.mybrain.superkassa.integrations.egovmobile

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class EgovMobileTest {
    private val document = EgovDocument(
        description = "Superkassa",
        nameRu = "Вход в кабинет БФД",
        nameKk = "БФД кабинетіне кіру",
        nameEn = "BFD cabinet sign-in"
    )
    private val qr = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
    private val sent = mutableListOf<String>()

    @Test
    fun signatureComesBackAsOneBase64Line() = runTest {
        val egov = relay(signed = signedWith("MIIB\\nAg=="))
        val procedure = egov.open("Y2hhbGxlbmdl", document)
        assertEquals("https://launch.egov/1", procedure.launch)
        assertContentEquals(qr, procedure.qr)
        assertEquals("MIIBAg==", egov.await(procedure))
        assertTrue(sent.single().contains("\"signMethod\":\"CMS_SIGN_ONLY\""))
        assertTrue(sent.single().contains("\"data\":\"Y2hhbGxlbmdl\""))
    }

    @Test
    fun ownerRefusalInEgovMobileIsCancelled() = runTest {
        val egov = relay(signed = """{"status":"CANCELED","documentsToSign":[]}""")
        val refusal = assertFailsWith<EgovRefusal> { egov.await(egov.open("AA==", document)) }
        assertEquals(EgovReason.Cancelled, refusal.reason)
    }

    @Test
    fun relayMessageIsRefusal() = runTest {
        val egov = EgovMobile(engine = MockEngine { json("""{"message":"too many requests"}""") })
        val refusal = assertFailsWith<EgovRefusal> { egov.open("AA==", document) }
        assertEquals(EgovReason.Refused, refusal.reason)
        assertEquals("too many requests", refusal.detail)
    }

    @Test
    fun silentRelayIsUnreachable() = runTest {
        val egov = EgovMobile(engine = MockEngine { throw IOException("no route") })
        val refusal = assertFailsWith<EgovRefusal> { egov.open("AA==", document) }
        assertEquals(EgovReason.Unreachable, refusal.reason)
    }

    @Test
    fun interruptedWaitIsRepeated() = runTest {
        var polls = 0
        val egov = relay(signed = signedWith("MIIC")) { polls++ == 0 }
        assertEquals("MIIC", egov.await(egov.open("AA==", document)))
        assertEquals(2, polls)
    }

    /** Посредник: регистрация, приём данных и ответ с подписью; [drop] обрывает долгий запрос. */
    private fun relay(signed: String, drop: () -> Boolean = { false }) = EgovMobile(
        settings = EgovSettings(relay = "https://relay.test", retryPause = 1.milliseconds),
        engine = MockEngine { request -> answer(request, signed, drop) }
    )

    private fun MockRequestHandleScope.answer(request: HttpRequestData, signed: String, drop: () -> Boolean) =
        when {
            request.url.encodedPath == "/api/egovQr" -> json(registered())
            request.method == HttpMethod.Post -> {
                sent += (request.body as TextContent).text
                json("""{"expireAt":1893456000000,"signURL":"https://relay.test/api/egovQr/7"}""")
            }
            drop() -> throw IOException("connection reset")
            else -> json(signed)
        }

    private fun signedWith(cms: String) =
        """{"documentsToSign":[{"id":1,"nameRu":"","nameKz":"","nameEn":"",""" +
            """"document":{"file":{"mime":"","data":"$cms"}}}]}"""

    private fun registered() = """{"expireAt":1893456000000,"qrCode":"${Base64.encode(qr)}",""" +
        """"eGovMobileLaunchLink":"https://launch.egov/1","dataURL":"https://relay.test/api/egovQr/7",""" +
        """"signURL":"https://relay.test/api/egovQr/7"}"""

    private fun MockRequestHandleScope.json(text: String): HttpResponseData =
        respond(text, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
}
