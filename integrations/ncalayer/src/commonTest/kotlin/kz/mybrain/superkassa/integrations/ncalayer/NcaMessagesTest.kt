package kz.mybrain.superkassa.integrations.ncalayer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.mybrain.superkassa.integrations.ncalayer.protocol.isGreeting
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaAddressee
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaLegacySignatureOf
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaSignRequest
import kz.mybrain.superkassa.integrations.ncalayer.protocol.ncaSignatureOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Сообщения NCALayer: что уходит на подпись и что считается подписью.
 *
 * Ключа у приложения нет, поэтому проверяется ровно то, за что отвечает
 * модуль: форма запроса и разбор ответа. Отказ владельца — обычный исход,
 * а не сбой, и назван отдельно.
 */
class NcaMessagesTest {

    private val request = ncaSignRequest("cGF5bG9hZA==", origin = "Superkassa", locale = "kk")

    private fun answer(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun requestCarriesContentFromServiceAsIs() {
        assertEquals("kz.gov.pki.knca.basics", request["module"]?.jsonPrimitive?.content)
        assertEquals("sign", request["method"]?.jsonPrimitive?.content)
        val args = request["args"]?.jsonObject
        assertEquals("cms", args?.get("format")?.jsonPrimitive?.content)
        assertEquals("cGF5bG9hZA==", args?.get("data")?.jsonPrimitive?.content)
        assertEquals("kk", args?.get("locale")?.jsonPrimitive?.content)
        assertEquals("Superkassa", request["origin"]?.jsonPrimitive?.content)
        assertEquals("kz.gov.pki.knca.basics/sign", ncaAddressee(request))
    }

    /** Служба проверяет подпись, подставляя данные сама: вложенное содержимое ей не нужно. */
    @Test
    fun signatureIsDetachedAndContentDecoded() {
        val params = request["args"]?.jsonObject?.get("signingParams")?.jsonObject
        assertEquals("true", params?.get("decode")?.jsonPrimitive?.content)
        assertEquals("false", params?.get("encapsulate")?.jsonPrimitive?.content)
        assertEquals("false", params?.get("digested")?.jsonPrimitive?.content)
    }

    @Test
    fun signatureIsTakenFromFirstResult() {
        assertEquals("MIIC-cms", ncaSignatureOf(answer("""{"status":true,"body":{"result":["MIIC-cms"]}}""")))
    }

    @Test
    fun ownersRefusalIsDeclinedNotConnectionFailure() {
        val cancelled = answer("""{"status":false,"message":"action.canceled"}""")
        val refusal = assertFailsWith<NcaRefusal> { ncaSignatureOf(cancelled) }
        assertEquals(NcaReason.Declined, refusal.reason)
        assertTrue(refusal.cancelled)
    }

    /** Приветствие с версией, принятое за ответ, выбрасывало пришедшую следом подпись. */
    @Test
    fun greetingIsNotAnswer() {
        assertTrue(answer("""{"result":{"version":"1.4"}}""").isGreeting())
        assertFalse(answer("""{"status":true,"body":{"result":["cms"]}}""").isGreeting())
    }

    @Test
    fun legacyModuleSignatureComesAsString() {
        assertEquals("MIIC-legacy", ncaLegacySignatureOf(answer("""{"code":"200","result":"MIIC-legacy"}""")))
        assertFailsWith<NcaRefusal> { ncaLegacySignatureOf(answer("""{"code":"500","message":"storage"}""")) }
    }

    @Test
    fun unknownAnswerReachesRefusalWhole() {
        val refusal = assertFailsWith<NcaRefusal> { ncaSignatureOf(answer("""{"result":{"version":"1.4"}}""")) }
        assertTrue(refusal.detail.contains("version"))
        assertTrue(refusal.askPreviousModule, "незнакомый ответ — повод спросить прежний модуль")
    }

    /** Строгий декодер службы отвергал подпись с переносами и обрамлением PEM. */
    @Test
    fun signatureIsCleanedOfLineBreaksAndPem() {
        val wrapped = """{"status":true,"body":{"result":
            ["-----BEGIN CMS-----\nMIIC+abc\ndef==\n-----END CMS-----"]}}"""
        assertEquals("MIIC+abcdef==", ncaSignatureOf(answer(wrapped)))
    }

    @Test
    fun emptyResultIsNoSignature() {
        assertFailsWith<NcaRefusal> { ncaSignatureOf(answer("""{"status":true,"body":{"result":[""]}}""")) }
    }

    /** Молчание ведёт к прежнему модулю, а закрытое окно и недоступность — нет. */
    @Test
    fun onlySilenceAsksPreviousModule() {
        assertTrue(ncaSilent().askPreviousModule)
        assertFalse(ncaWindowClosed().askPreviousModule)
        assertFalse(ncaUnreachable().askPreviousModule)
        assertEquals(NcaReason.Unreachable, ncaUnreachable().reason)
        assertEquals(NcaReason.Silent, ncaSilent().reason)
    }
}
