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
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSalesModel
import kz.mybrain.superkassa.desktop.ui.analytics.regionsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Свод по регионам собирается и у владельца, открывшего аналитику первой.
 *
 * Регион стоит в адресе торговой точки, а в строках сводки адреса нет:
 * без справочника точек весь свод сходился в одну строку «Без адреса»
 * со ста процентами сети. Справочник читал только раздел торговых точек
 * кабинета, и сводка молча полагалась на то, что владелец туда заходил.
 */
class AnalyticsSalesRegionsSourceTest {

    @Test
    fun `сводка сама читает справочник точек`() {
        val asked = mutableListOf<String>()
        val cabinet = mockCabinet(clientRecording(asked))
        val model = AnalyticsSalesModel(cabinet)
        runBlocking { model.load() }

        assertTrue(
            asked.any { it.startsWith("/api/retail-places") },
            "справочник точек сводка не спросила: $asked"
        )
        assertEquals(listOf("Алматы"), cabinet.places.map { it.address?.substringBefore(",") })

        val view = requireNonNull(model.view)
        val regions = regionsOf(view.places, view.registers, view.retailPlaces, NO_ADDRESS)
        assertEquals(listOf("Алматы"), regions.map { it.title }, "свод сошёлся не в область, а в прочерк")
    }

    private fun <T : Any> requireNonNull(value: T?): T = requireNotNull(value) { "сводка не собралась" }

    /** Кабинет, отвечающий на каждую ручку сводки своим телом; спрошенное запоминается. */
    private fun clientRecording(asked: MutableList<String>): CabinetClient {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            asked += path
            respond(bodyFor(path), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    private fun bodyFor(path: String): String = when {
        path.startsWith("/api/retail-places") -> PLACES
        path.endsWith("/by-retail-place") -> SOLD
        path.endsWith("/by-cash-register") -> SOLD
        path.endsWith("/summary") -> SUMMARY
        else -> "{}"
    }

    private companion object {
        const val NO_ADDRESS = "Без адреса"

        const val PLACES = """{"totalElements":1,"items":[{"id":"p1","name":"Магазин на Достык",
            "address":"Алматы, Медеуский, Достык, 10"}]}"""

        const val SOLD = """{"retailPlaces":[{"retailPlaceId":"p1","name":"Магазин на Достык",
            "retailPlaceName":"Магазин на Достык","receiptCount":81,"revenue":61820.00}]}"""

        const val SUMMARY = """{"receiptCount":81,"revenue":61820.00,"cashRegisterCount":3294}"""
    }
}
