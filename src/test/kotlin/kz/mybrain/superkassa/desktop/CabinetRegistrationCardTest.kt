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
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCardVersion
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCardVersions
import kz.mybrain.superkassa.desktop.ui.cabinet.cardFieldTitle
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Версии регистрационной карты: разбор ответа и пути обращений.
 *
 * Карта отражает состояние кассы на сегодня, и перерегистрация переписывает
 * в ней адрес и точку. Подтверждают же обычно прежнюю запись — ту, что
 * действовала в спрашиваемые дни. Кабинет версии отдаёт, приложение
 * показывало только действующую.
 *
 * Имена полей у кабинета ещё сдвинутся, поэтому разбор снисходительный:
 * здесь проверено, что оба написания читаются одинаково.
 */
class CabinetRegistrationCardTest {

    private var seenPath: String = ""

    private fun clientReturning(body: String): CabinetClient {
        val engine = MockEngine { request ->
            seenPath = request.url.encodedPath + (request.url.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    @Test
    fun `список версий читается вместе со сроком, действиями и изменённым`() {
        val client = clientReturning(
            """{"page":0,"size":50,"totalElements":2,"items":[
               {"version":1,"status":"REGISTERED","openedAt":"2026-03-01T10:00:00Z","closedAt":"2026-09-18T12:00:00Z",
                "openedByActionType":"REGISTRATION","closedByActionType":"REREGISTRATION","changedFields":[]},
               {"version":2,"status":"REGISTERED","openedAt":"2026-09-18T12:00:00Z","closedAt":null,
                "openedByActionType":"REREGISTRATION","changedFields":["ADDRESS","RETAIL_PLACE"],"active":true}]}"""
        )
        val page = runBlocking { client.registrationCardVersions("токен", "07aeaff1") }
        assertEquals(2, page.size)
        val first = page.first()
        assertEquals(1, first.version)
        assertEquals("REGISTRATION", first.openedBy)
        assertEquals("REREGISTRATION", first.closedBy)
        assertTrue(!first.open, "закрытая версия названа действующей")

        val last = page.last()
        assertEquals(listOf("ADDRESS", "RETAIL_PLACE"), last.changed)
        assertTrue(last.open, "действующая версия названа закрытой")
        assertTrue(seenPath.startsWith("/api/cash-registers/07aeaff1/registration-card/versions"), seenPath)
    }

    /**
     * Кабинет отдаёт версии голым массивом. Пока приложение ждало конверт,
     * раздел показывал владельцу текст исключения разбора вместо списка.
     */
    @Test
    fun `версии читаются и голым массивом`() {
        val client = clientReturning(
            """[{"version":1,"openedByActionType":"REGISTRATION","openedAt":"2026-09-18T12:00:00Z","active":true}]"""
        )

        val versions = runBlocking { client.registrationCardVersions("токен", "07aeaff1") }

        assertEquals(1, versions.single().version)
        assertTrue(versions.single().open)
    }

    @Test
    fun `второе написание полей читается так же, как первое`() {
        val client = clientReturning(
            """{"items":[{"versionNumber":3,"createdAt":"2026-09-18T12:00:00Z",
               "openAction":"REREGISTRATION","changes":["MODEL"],"isCurrent":true}]}"""
        )
        val version = runBlocking { client.registrationCardVersions("токен", "07aeaff1") }.single()
        assertEquals(3, version.version)
        assertEquals("2026-09-18T12:00:00Z", version.validFrom)
        assertEquals("REREGISTRATION", version.openedBy)
        assertEquals(listOf("MODEL"), version.changed)
        assertTrue(version.open)
    }

    @Test
    fun `карта нужной версии спрашивается по её номеру`() {
        val client = clientReturning(
            """{"cashRegisterId":"07aeaff1","registrationNumber":"000000000123",
               "retailPlaceName":"Магазин на Абая","address":"Алматы, Абая, 10","modelName":"ПОРТ FPG-350 ФKZ"}"""
        )
        val card = runBlocking { client.registrationCardVersion("токен", "07aeaff1", 2) }
        assertEquals("Алматы, Абая, 10", card.address)
        assertEquals("/api/cash-registers/07aeaff1/registration-card/versions/2", seenPath)
    }

    @Test
    fun `изменённое названо словами, а незнакомый код показан как пришёл`() {
        val texts = cabinetTexts(Language.Ru)
        assertEquals(texts.address, cardFieldTitle("ADDRESS", texts))
        assertEquals(texts.placeName, cardFieldTitle("RETAIL_PLACE", texts))
        assertEquals(texts.model, cardFieldTitle("KKM_MODEL", texts))
        assertEquals("СОВСЕМ_НОВОЕ_ПОЛЕ", cardFieldTitle("СОВСЕМ_НОВОЕ_ПОЛЕ", texts))
    }
}
