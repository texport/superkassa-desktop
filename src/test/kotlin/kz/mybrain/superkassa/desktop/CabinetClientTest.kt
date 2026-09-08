package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRefusal
import kz.mybrain.superkassa.desktop.server.cabinet.edsChallenge
import kz.mybrain.superkassa.desktop.server.cabinet.me
import kz.mybrain.superkassa.desktop.server.cabinet.registers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Обмен с личным кабинетом ОФД.
 *
 * Ответы ниже сняты с контракта кабинета (`docs/openapi.yaml`). Проверяется
 * то, из-за чего раздел показал бы неверное: разбор страницы со списком
 * и отделение отказа по существу от истёкшего доступа — по второму
 * владельца возвращает ко входу, а не оставляет с пустым списком.
 */
class CabinetClientTest {

    private fun clientReturning(status: HttpStatusCode, body: String): CabinetClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    @Test
    fun `задача на подпись разбирается целиком`() {
        val client = clientReturning(
            HttpStatusCode.OK,
            """{"challengeId":"11111111-1111-1111-1111-111111111111","payload":"cGF5bG9hZA==",
               "expiresAt":"2026-09-07T19:00:00Z"}"""
        )
        val challenge = runBlocking { client.edsChallenge() }
        assertEquals("cGF5bG9hZA==", challenge.payload)
        assertEquals("11111111-1111-1111-1111-111111111111", challenge.challengeId)
    }

    @Test
    fun `список касс приходит страницей, а не голым массивом`() {
        val client = clientReturning(
            HttpStatusCode.OK,
            """{"page":0,"size":50,"totalElements":1,"items":[
               {"id":"22222222-2222-2222-2222-222222222222","kkmId":2000302,"status":"REGISTERED",
                "registrationNumber":"KGD-2000302","factoryNumber":"KZT26E2C509A200","manufactureYear":2026,
                "model":{"modelCode":"M1","name":"Суперкасса"},
                "retailPlace":{"id":"33333333-3333-3333-3333-333333333333","name":"Магазин у дома"}}]}"""
        )
        val page = runBlocking { client.registers("token") }
        assertEquals(1, page.items.size)
        assertEquals("KGD-2000302", page.items.single().registrationNumber)
        assertEquals("Магазин у дома", page.items.single().retailPlace?.name)
    }

    @Test
    fun `истёкший доступ отличается от отказа по существу`() {
        val expired = clientReturning(HttpStatusCode.Unauthorized, """{"code":"UNAUTHORIZED","message":"Сессия истекла"}""")
        val refusal = assertFailsWith<CabinetRefusal> { runBlocking { expired.me("token") } }
        assertEquals(HttpStatusCode.Unauthorized.value, refusal.httpStatus)
        assertEquals("UNAUTHORIZED", refusal.code)
    }

    @Test
    fun `отказ без кода назван состоянием ответа`() {
        val broken = clientReturning(HttpStatusCode.BadGateway, "<html>gateway</html>")
        val refusal = assertFailsWith<CabinetRefusal> { runBlocking { broken.me("token") } }
        assertEquals("HTTP_502", refusal.code)
    }
}
