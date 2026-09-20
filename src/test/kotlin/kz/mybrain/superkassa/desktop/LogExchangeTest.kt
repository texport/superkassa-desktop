package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogJournal
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Обмен с узлом и с кабинетом в журнале.
 *
 * Проверяется то, ради чего журнал и заводился: обращение видно целиком,
 * отказ отличим от сбоя службы, а пин, ушедший в заголовке, в журнал
 * не попадает. Отдельно — что запись тела не съедает сам ответ: журнал,
 * ломающий разбор ответа, хуже отсутствующего.
 */
class LogExchangeTest {

    @BeforeTest
    fun freshJournal() {
        AppLog.journal = LogJournal(level = LogLevel.Debug)
    }

    private fun http(body: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    ) {
        expectSuccess = false
        install(ContentNegotiation) { json(ServerClient.lenientJson) }
    }

    @Test
    fun `обращение к узлу записывается методом, путём, кодом и временем`() = runBlocking {
        val client = ServerClient(address = { "http://node" }, http = http("""{"kkmId":"7"}"""))

        client.call(HttpMethod.Get, "/api/v1/kkms", body = null, pin = "1234")

        val entry = AppLog.entries.single()
        assertEquals(LogSource.Node, entry.source)
        assertEquals(LogLevel.Info, entry.level)
        assertTrue(entry.text.startsWith("GET /api/v1/kkms -> 200"), entry.text)
        assertTrue(entry.text.contains("мс"), entry.text)
        assertFalse(entry.line().contains("1234"), "пин ушёл заголовком и в журнал не попадает")
    }

    /**
     * Ответ остаётся читаемым после того, как журнал взял из него тело.
     * Иначе включённая отладка ломала бы разбор каждого ответа — и первым
     * это увидел бы кассир.
     */
    @Test
    fun `тело ответа читается и после записи в журнал`() = runBlocking {
        val client = ServerClient(address = { "http://node" }, http = http("""{"kkmId":"7"}"""))

        val response = client.call(HttpMethod.Get, "/api/v1/kkms", body = null, pin = null)
        val parsed: JsonObject = response.body()

        assertEquals("7", parsed["kkmId"]?.jsonPrimitive?.content)
        assertEquals("""{"kkmId":"7"}""", response.bodyAsText())
        assertTrue(AppLog.entries.single().body.orEmpty().contains("kkmId"), "тело ответа в журнале")
    }

    /**
     * Адреса, с которых кассы выходят на связь, в журнал не пишутся даже
     * на отладочном уровне: журнал владелец отправляет в поддержку.
     */
    @Test
    fun `адреса обмена касс в журнал не попадают`() = runBlocking {
        val answer = """{"addresses":[{"address":"188.94.159.10","lastSeen":"2026-09-19T10:00:00Z"}]}"""
        val client = ServerClient(address = { "http://cabinet" }, http = http(answer))

        client.call(HttpMethod.Get, "/api/analytics/cash-registers/addresses", body = null, pin = null)

        val entry = AppLog.entries.single()
        assertTrue(entry.text.contains("/addresses -> 200"), entry.text)
        assertFalse(entry.line().contains("188.94.159.10"), "адрес обмена в журнале")
    }

    /** Отказ по существу — предупреждение: служба ответила. */
    @Test
    fun `отказ узла записывается предупреждением, а сбой службы — отказом`() = runBlocking {
        val refusing = ServerClient(
            address = { "http://node" },
            http = http("""{"code":"SHIFT_NOT_OPEN"}""", HttpStatusCode.Conflict)
        )
        refusing.call(HttpMethod.Post, "/api/v1/tickets", body = null, pin = null)
        assertEquals(LogLevel.Warning, AppLog.entries.single().level)

        freshJournal()
        val broken = ServerClient(
            address = { "http://node" },
            http = http("{}", HttpStatusCode.InternalServerError)
        )
        broken.call(HttpMethod.Post, "/api/v1/tickets", body = null, pin = null)
        assertEquals(LogLevel.Failure, AppLog.entries.single().level)
    }

    /** Кабинет пишется своим источником: разговоров два, и они не смешиваются. */
    @Test
    fun `обращение к кабинету записывается источником кабинета`() = runBlocking {
        val client = CabinetClient(baseUrl = "http://cabinet", http = http("""{"bin":"123456789012"}"""))

        client.call(HttpMethod.Get, "/api/v1/me", body = null, token = "secret-token")

        val entry = AppLog.entries.single()
        assertEquals(LogSource.Cabinet, entry.source)
        assertTrue(entry.text.startsWith("GET /api/v1/me -> 200"), entry.text)
        assertFalse(entry.line().contains("secret-token"), "доступ кабинета в журнал не попадает")
    }
}
