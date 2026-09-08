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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Разбор отказа узла.
 *
 * Узел склеивает три языка в одну строку. Кассиру нужна русская часть, а код
 * отказа нужен поддержке: по нему видно «недостаточно наличных» против
 * «касса заблокирована», не читая журналы.
 */
class ServerRefusalTest {

    private fun clientReturning(status: HttpStatusCode, body: String): ServerClient {
        val engine = MockEngine { _ ->
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        return ServerClient(http = http)
    }

    @Test
    fun `из трёхъязычного текста берётся русская часть`() = runBlocking {
        val body = """{"code":"INSUFFICIENT_CASH","message":"RU: В кассе недостаточно наличных | """ +
            """KK: Кассада жеткіліксіз | EN: Not enough cash"}"""
        val client = clientReturning(HttpStatusCode.BadRequest, body)

        val response = client.call(HttpMethod.Get, "/kkm", null, null)
        val refusal = client.refusalOf(response)

        assertEquals("INSUFFICIENT_CASH", refusal.code)
        assertEquals("В кассе недостаточно наличных", refusal.russianText)
        assertEquals(HttpStatusCode.BadRequest.value, refusal.httpStatus)
    }

    @Test
    fun `отказ без кода получает код по состоянию ответа`() = runBlocking {
        val client = clientReturning(HttpStatusCode.InternalServerError, "сломалось")

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("HTTP_500", refusal.code)
        assertEquals("сломалось", refusal.russianText)
    }

    @Test
    fun `однуязычное сообщение показывается целиком`() = runBlocking {
        val body = """{"code":"KKM_BLOCKED","message":"Касса заблокирована"}"""
        val client = clientReturning(HttpStatusCode.Conflict, body)

        val refusal = client.refusalOf(client.call(HttpMethod.Get, "/kkm", null, null))

        assertEquals("Касса заблокирована", refusal.russianText)
    }
}
