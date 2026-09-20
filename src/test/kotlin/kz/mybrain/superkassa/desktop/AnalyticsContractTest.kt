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
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.server.cabinet.cashRegisterMap
import kz.mybrain.superkassa.desktop.server.cabinet.exchangeAddresses
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.analyticsTrouble
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ответы аналитики кабинета разбираются в модели приложения.
 *
 * Тела написаны по контракту, согласованному со стороной кабинета:
 * ручек в выложенном кабинете ещё нет, и до выкладки этот тест —
 * единственное место, где контракт проверяется целиком. Поменяется
 * схема — он покажет, что именно перестало читаться.
 */
class AnalyticsContractTest {

    private var seenPath: String = ""

    private fun clientReturning(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetClient {
        val engine = MockEngine { request ->
            seenPath = request.url.encodedPath + "?" + request.url.encodedQuery
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    @Test
    fun `карта касс разбирается вместе с положением и счётчиками`() {
        val client = clientReturning(
            """{"positionSource":"KKM_COORDINATES","placedCount":1,"withoutPositionCount":1,
               "placed":[{"cashRegisterId":"c1","kkmId":2000302,"registrationNumber":"KGD-2000302",
                 "internalName":"Касса у входа","retailPlaceId":"p1","retailPlaceName":"Магазин на Абая",
                 "address":"г. Алматы, пр. Абая, 10","status":"REGISTERED","blocked":false,
                 "shiftStatus":"OPEN","shiftNumber":12,"lastContactAt":"2026-09-19T08:14:00Z",
                 "position":{"source":"KKM_COORDINATES","latitude":43.238949,"longitude":76.889709,
                   "geoSource":"GNSS","rka":"1234567","cato":"750000000"}}],
               "withoutPosition":[{"cashRegisterId":"c2","kkmId":2000303,"status":"DRAFT","blocked":false}]}"""
        )
        val view = runBlocking { client.cashRegisterMap("token", PositionSource.KkmCoordinates) }
        assertEquals(PositionSource.KkmCoordinates, view.source)
        assertEquals(1, view.placedCount)
        assertEquals(1, view.withoutPositionCount)
        val placed = view.placed.single()
        assertEquals("Касса у входа", placed.internalName)
        assertEquals(12L, placed.shiftNumber)
        assertEquals("GNSS", placed.position?.geoSource)
        assertTrue(placed.position?.degrees == true)
        assertEquals("c2", view.withoutPosition.single().cashRegisterId)
    }

    @Test
    fun `при адресе торговой точки координат нет, а адрес с РКА и САТО есть`() {
        val client = clientReturning(
            """{"positionSource":"RETAIL_PLACE_ADDRESS","placedCount":1,"withoutPositionCount":0,
               "placed":[{"cashRegisterId":"c1","kkmId":2000302,"address":"г. Актобе, ул. Абилкайыр хана, 40",
                 "position":{"source":"RETAIL_PLACE_ADDRESS","rka":"7654321","cato":"151010000"}}],
               "withoutPosition":[]}"""
        )
        val view = runBlocking { client.cashRegisterMap("token", PositionSource.RetailPlaceAddress) }
        val position = view.placed.single().position
        assertNull(position?.latitude)
        assertNull(position?.longitude)
        assertEquals("7654321", position?.rka)
        assertEquals("151010000", position?.cato)
        assertTrue(position?.degrees == false)
    }

    @Test
    fun `источник положения уходит строкой запроса`() {
        val client = clientReturning("""{"placed":[],"withoutPosition":[]}""")
        runBlocking { client.cashRegisterMap("token", PositionSource.CabinetCoordinates, retailPlaceId = "p 1") }
        assertTrue(seenPath.contains("positionSource=CABINET_COORDINATES"), seenPath)
        assertTrue(seenPath.contains("retailPlaceId=p+1"), seenPath)
        assertTrue(!seenPath.contains("status="), seenPath)
    }

    @Test
    fun `адреса обмена разбираются со счётчиками и временем`() {
        val client = clientReturning(
            """{"cashRegisterCount":2,"addressCount":3,"addresses":[
               {"cashRegisterId":"c1","kkmId":2000302,"registrationNumber":"KGD-2000302",
                "internalName":"Касса у входа","retailPlaceName":"Магазин на Абая","address":"212.154.10.7",
                "firstSeen":"2026-09-01T06:00:00Z","lastSeen":"2026-09-19T08:14:00Z"}]}"""
        )
        val view = runBlocking { client.exchangeAddresses("token") }
        assertEquals(2, view.cashRegisterCount)
        assertEquals(3, view.addressCount)
        assertEquals("212.154.10.7", view.addresses.single().address)
    }

    @Test
    fun `у кассы без обменов пустой список и нули, а не отказ`() {
        val client = clientReturning("""{"cashRegisterCount":1,"addressCount":0,"addresses":[]}""")
        val view = runBlocking { client.exchangeAddresses("token", "c1") }
        assertEquals(0, view.addressCount)
        assertTrue(view.addresses.isEmpty())
        assertTrue(seenPath.startsWith("/api/analytics/cash-registers/c1/addresses"), seenPath)
    }

    @Test
    fun `404 до выкладки кабинета — не отказ, а отсутствующий раздел`() {
        val client = clientReturning("""{"title":"Not Found"}""", HttpStatusCode.NotFound)
        val trouble = runCatching {
            runBlocking { client.cashRegisterMap("token", PositionSource.RetailPlaceAddress) }
        }.exceptionOrNull()?.let(::analyticsTrouble)
        assertEquals(AnalyticsTrouble.NotDeployed, trouble)
    }

    @Test
    fun `отказ по существу доходит своими словами, а молчание — отдельным случаем`() {
        val client = clientReturning("""{"code":"FORBIDDEN","message":"no access"}""", HttpStatusCode.Forbidden)
        val refused = runCatching {
            runBlocking { client.exchangeAddresses("token") }
        }.exceptionOrNull()?.let(::analyticsTrouble)
        assertEquals(AnalyticsTrouble.Refused("no access"), refused)
        assertTrue(analyticsTrouble(IllegalStateException("порт закрыт")) is AnalyticsTrouble.Unreachable)
    }
}
