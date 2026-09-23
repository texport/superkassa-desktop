package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Разбор отказа узла.
 *
 * Узел склеивает три языка в одну строку. Кассиру нужна русская часть, а код
 * отказа нужен поддержке: по нему видно «недостаточно наличных» против
 * «касса заблокирована», не читая журналы.
 */
class ServerRefusalTest {

    private fun clientReturning(
        status: HttpStatusCode,
        body: String,
        type: String = "application/json"
    ): ServerClient {
        val engine = MockEngine { _ ->
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, type)
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        return ServerClient(http = http)
    }

    @Test
    fun `трёхъязычный текст разбирается по языкам`() = runBlocking {
        val body = """{"code":"INSUFFICIENT_CASH","message":"RU: В кассе недостаточно наличных | """ +
            """KK: Кассада жеткіліксіз | EN: Not enough cash"}"""
        val client = clientReturning(HttpStatusCode.BadRequest, body)

        val response = client.call(HttpMethod.Get, "/kkm", null, null)
        val refusal = client.refusalOf(response)

        assertEquals("INSUFFICIENT_CASH", refusal.code)
        assertEquals("В кассе недостаточно наличных", refusal.words.of(Language.Ru))
        assertEquals("Кассада жеткіліксіз", refusal.words.of(Language.Kk))
        assertEquals("Not enough cash", refusal.words.of(Language.En))
        assertEquals(HttpStatusCode.BadRequest.value, refusal.httpStatus)
    }

    @Test
    fun `отказ без кода получает код по состоянию ответа`() = runBlocking {
        val client = clientReturning(HttpStatusCode.InternalServerError, "сломалось")

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("HTTP_500", refusal.code)
        assertEquals("сломалось", refusal.answer, "ответ узла обязан дойти до журнала целиком")
    }

    /**
     * Отказ, тело которого разобрать нечем.
     *
     * Так отвечает промежуточное звено на пути к узлу, и на экране кассира
     * оказывалось английское «Not Found». Кассир английских отказов читать
     * не обязан: слова ему даются свои и на всех трёх языках, а ответ узла
     * остаётся журналу.
     */
    @Test
    fun `отказ без разбираемого тела объясняется своими словами`() = runBlocking {
        val client = clientReturning(HttpStatusCode.NotFound, "Not Found", type = "text/plain")

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("HTTP_404", refusal.code)
        assertEquals("Not Found", refusal.answer, "ответ узла обязан дойти до журнала")
        val words = Language.entries.map { refusal.words.of(it) }
        words.forEach { said ->
            assertFalse(said.contains("Not Found"), "ответ узла дошёл до кассира как есть: $said")
            assertTrue(said.isNotBlank(), "кассиру не сказано ничего")
        }
        assertEquals(Language.entries.size, words.distinct().size, "отказ объяснён не на всех трёх языках")
    }

    @Test
    fun `пустое тело отказа тоже объясняется словами, а журналу нечего сказать`() = runBlocking {
        val client = clientReturning(HttpStatusCode.BadGateway, "", type = "text/plain")

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("HTTP_502", refusal.code)
        assertNull(refusal.answer, "пустой ответ журналу нечего показывать")
        assertTrue(refusal.words.of(Language.Kk).isNotBlank(), "кассиру не сказано ничего")
    }

    @Test
    fun `сбой узла помечает языки скобками и тоже разбирается`() = runBlocking {
        // Второй способ разметки: так узел отвечает о собственных сбоях.
        // Приложение его не знало, и кассир читал все три языка сразу.
        val body = """{"code":"INTERNAL_ERROR","message":"[EN] Internal server error / """ +
            """[RU] Внутренняя ошибка сервера / [KK] Сервердің ішкі қатесі"}"""
        val client = clientReturning(HttpStatusCode.InternalServerError, body)

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("INTERNAL_ERROR", refusal.code)
        assertEquals("Внутренняя ошибка сервера", refusal.words.of(Language.Ru))
        assertEquals("Сервердің ішкі қатесі", refusal.words.of(Language.Kk))
        assertEquals("Internal server error", refusal.words.of(Language.En))
    }

    @Test
    fun `однуязычное сообщение показывается целиком`() = runBlocking {
        val body = """{"code":"KKM_BLOCKED","message":"Касса заблокирована"}"""
        val client = clientReturning(HttpStatusCode.Conflict, body)

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("Касса заблокирована", refusal.words.of(Language.Ru))
        // Языка в сообщении нет — на казахском показывается тот же текст,
        // а не пустая строка: молчание хуже чужого языка.
        assertEquals("Касса заблокирована", refusal.words.of(Language.Kk))
    }
}
