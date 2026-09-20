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
import kz.mybrain.superkassa.desktop.server.cabinet.AddressSuggestion
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.addressBuildings
import kz.mybrain.superkassa.desktop.ui.cabinet.distinctSuggestions
import kz.mybrain.superkassa.desktop.ui.map.mapFollowsAddress
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.mapAddressTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Связка окна карты с адресным регистром кабинета.
 *
 * Координаты точки даёт карта, адрес — регистр, и разойтись им нельзя:
 * прежде адрес брался из регистра, а координаты — из постороннего поиска
 * по набранному руками тексту.
 */
class MapRegistryAddressTest {

    @Test
    fun `смена адреса ведёт карту к новому дому`() {
        assertTrue(mapFollowsAddress("Алматы, Медеуский, Достык, 10", "0201300118176503", "2201300111910697", true))
    }

    @Test
    fun `у адреса, с которым окно открылось, метка остаётся на месте`() {
        assertFalse(mapFollowsAddress("Алматы, Медеуский, Достык, 10", "0201300118176503", "0201300118176503", true))
    }

    @Test
    fun `без метки карта идёт к выбранному адресу`() {
        assertTrue(mapFollowsAddress("Алматы, Медеуский, Достык, 10", "0201300118176503", "0201300118176503", false))
    }

    @Test
    fun `без адреса карта никуда не идёт`() {
        assertFalse(mapFollowsAddress("", null, null, false))
    }

    @Test
    fun `надписи связки заполнены на каждом языке`() {
        Language.entries.forEach { language ->
            val texts = mapAddressTexts(language)
            listOf(texts.pickAddressFirst, texts.searching, texts.notOnMap).forEach {
                assertTrue(it.isNotBlank(), "$language: пустая надпись связки карты с регистром")
            }
        }
    }

    /**
     * Двойники номеров домов приходят от регистра, а не склеиваются в приложении.
     *
     * Тело снято с `bfd-cabinet.ecc.kz` 2026-09-18 запросом
     * `/api/reference/addresses/buildings?geonimId=143546&number=10`:
     * на каждый номер регистр отдаёт две записи с разными кодами РКА.
     */
    @Test
    fun `регистр отдаёт на номер дома две записи с разными РКА`() {
        val body = """{"items":[
            {"id":4631988,"name":"10","rka":"0201300118176503","level":"BUILDING"},
            {"id":1119106,"name":"10","rka":"2201300111910697","level":"BUILDING"},
            {"id":4613198,"name":"101","rka":"0201300116990906","level":"BUILDING"},
            {"id":1107512,"name":"101","rka":"2201300110751294","level":"BUILDING"}]}"""
        val items = runBlocking { clientReturning(body).addressBuildings("token", 143546, "10").items }

        assertEquals(listOf("10", "10", "101", "101"), items.map { it.name })
        assertEquals(4, items.mapNotNull { it.rka }.distinct().size)
        assertEquals(items, distinctSuggestions(items))
    }

    @Test
    fun `полный двойник из списка убирается`() {
        val twins = listOf(
            AddressSuggestion(id = 1, name = "10", rka = "0201300118176503", level = "BUILDING"),
            AddressSuggestion(id = 2, name = "10", rka = "0201300118176503", level = "BUILDING")
        )

        assertEquals(1, distinctSuggestions(twins).size)
    }

    private fun clientReturning(body: String): CabinetClient {
        val engine = MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }
}
