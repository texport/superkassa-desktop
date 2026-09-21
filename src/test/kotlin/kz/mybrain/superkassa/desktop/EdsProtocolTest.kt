package kz.mybrain.superkassa.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.mybrain.superkassa.desktop.eds.EdsProblem
import kz.mybrain.superkassa.desktop.eds.EdsRefusal
import kz.mybrain.superkassa.desktop.eds.ncaUnreachable
import kz.mybrain.superkassa.desktop.eds.ncaIsGreeting
import kz.mybrain.superkassa.desktop.eds.ncaLegacySignatureOf
import kz.mybrain.superkassa.desktop.eds.ncaSignRequest
import kz.mybrain.superkassa.desktop.eds.ncaSignatureOf
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Обмен с NCALayer: что уходит на подпись и что считается подписью.
 *
 * Ключа у приложения нет, поэтому проверяется ровно то, за что оно
 * отвечает: форма запроса модулю `basics` и разбор ответа. Отказ владельца
 * от подписи — обычный исход, а не сбой, и назван отдельно.
 */
class EdsProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Первое подключение к NCALayer на Linux срывается, второе проходит.
     * Повторять можно только пока запрос не ушёл: NCALayer его не видел
     * и окна подписи не открывал. После отправки повтор открыл бы окно
     * второй раз.
     */
    @Test
    fun `сорванное подключение можно повторить, а сорванный обмен нельзя`() {
        val onConnect = ncaUnreachable(SSLException("handshake_failure"), sent = false)
        val onExchange = ncaUnreachable(SSLException("handshake_failure"), sent = true)

        assertTrue(onConnect.beforeRequest)
        assertFalse(onExchange.beforeRequest)
    }

    /**
     * У отказов защищённого соединения сообщение бывает пустым, и в журнале
     * оставалось «подпись не получена: Unreachable». Разбирают такое
     * по журналу с чужой машины, и имя исключения там единственная зацепка.
     */
    @Test
    fun `в подробностях отказа стоит имя исключения`() {
        assertEquals("SSLException", ncaUnreachable(SSLException(null as String?), sent = false).detail)
        assertEquals(
            "SocketTimeoutException: connect timed out",
            ncaUnreachable(SocketTimeoutException("connect timed out"), sent = false).detail
        )
    }

    @Test
    fun `на подпись уходит содержимое от кабинета, а не его пересказ`() {
        val request = ncaSignRequest("cGF5bG9hZA==")
        assertEquals("kz.gov.pki.knca.basics", request["module"]?.jsonPrimitive?.content)
        assertEquals("sign", request["method"]?.jsonPrimitive?.content)
        val args = request["args"]?.jsonObject
        assertEquals("cms", args?.get("format")?.jsonPrimitive?.content)
        assertEquals("cGF5bG9hZA==", args?.get("data")?.jsonPrimitive?.content)
    }

    @Test
    fun `запрос называет приложение, а не остаётся неопознанным`() {
        val request = ncaSignRequest("cGF5bG9hZA==")
        // Без этого поля NCALayer пишет владельцу «UNIDENTIFIED запрашивает
        // разрешение»: имя просителя модуль подписи берёт из запроса,
        // а не из соединения.
        assertEquals("Superkassa", request["origin"]?.jsonPrimitive?.content)
    }

    @Test
    fun `подпись просится отсоединённой и с развёрнутым base64`() {
        // Кабинет проверяет подпись, подставляя данные сам: вложенное содержимое ему не нужно.
        val params = ncaSignRequest("cGF5bG9hZA==")["args"]?.jsonObject?.get("signingParams")?.jsonObject
        assertEquals("true", params?.get("decode")?.jsonPrimitive?.content)
        assertEquals("false", params?.get("encapsulate")?.jsonPrimitive?.content)
        assertEquals("false", params?.get("digested")?.jsonPrimitive?.content)
    }

    @Test
    fun `подпись берётся из первого результата ответа`() {
        val answer = json.parseToJsonElement(
            """{"status":true,"body":{"result":["MIIC-cms"]}}"""
        ).jsonObject
        assertEquals("MIIC-cms", ncaSignatureOf(answer))
    }

    @Test
    fun `отказ владельца назван отказом, а не сбоем связи`() {
        val answer = json.parseToJsonElement(
            """{"status":false,"message":"action.canceled"}"""
        ).jsonObject
        val refusal = assertFailsWith<EdsRefusal> { ncaSignatureOf(answer) }
        assertEquals(EdsProblem.Declined, refusal.problem)
        assertTrue(refusal.detail.contains("canceled"))
    }

    @Test
    fun `приветствие NCALayer ответом на подпись не считается`() {
        // NCALayer здоровается первым кадром со своей версией. Приложение
        // принимало его за ответ, выбрасывало пришедшую следом подпись
        // и открывало окно подписи во второй раз.
        val greeting = json.parseToJsonElement("""{"result":{"version":"1.4"}}""").jsonObject
        assertTrue(ncaIsGreeting(greeting))
        val answer = json.parseToJsonElement("""{"status":true,"body":{"result":["cms"]}}""").jsonObject
        assertTrue(!ncaIsGreeting(answer))
    }

    @Test
    fun `подпись прежнего модуля приходит строкой`() {
        val answer = json.parseToJsonElement("""{"code":"200","result":"MIIC-legacy"}""").jsonObject
        assertEquals("MIIC-legacy", ncaLegacySignatureOf(answer))
    }

    @Test
    fun `отказ незнакомой формы доходит до экрана целиком`() {
        val answer = json.parseToJsonElement("""{"result":{"version":"1.4"}}""").jsonObject
        val refusal = assertFailsWith<EdsRefusal> { ncaSignatureOf(answer) }
        assertTrue(refusal.detail.contains("version"))
    }

    @Test
    fun `подпись очищается от переносов и обрамления PEM`() {
        // NCALayer переносит строки, а иногда обрамляет подпись PEM.
        // Кабинет разбирает строгим декодером и отвечал на такую строку
        // «signatureCms is not valid base64».
        val wrapped = """{"status":true,"body":{"result":
            ["-----BEGIN CMS-----\nMIIC+abc\ndef==\n-----END CMS-----"]}}"""
        val answer = json.parseToJsonElement(wrapped).jsonObject
        assertEquals("MIIC+abcdef==", ncaSignatureOf(answer))
    }

    @Test
    fun `пустой результат подписью не считается`() {
        val answer = json.parseToJsonElement("""{"status":true,"body":{"result":[""]}}""").jsonObject
        assertFailsWith<EdsRefusal> { ncaSignatureOf(answer) }
    }
}
