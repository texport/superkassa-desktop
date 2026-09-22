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
import kz.mybrain.superkassa.desktop.ui.analytics.AddressAnswer
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsMapModel
import kz.mybrain.superkassa.desktop.ui.analytics.addressesToFind
import kz.mybrain.superkassa.desktop.ui.analytics.placement
import kz.mybrain.superkassa.desktop.ui.map.MapGeocoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Карта заполняется сразу, а не через четверть часа.
 *
 * Кабинет отдаёт координаты готовыми только при источнике «по кабинету».
 * При адресе торговой точки координат в ответе нет вовсе, и дом по каждому
 * адресу ищет открытая служба карт — по одному в секунду с обязательной
 * паузой. Раздел открывался именно на нём, и сеть из тысячи разных адресов
 * вставала бы на карту четверть часа, а гость всё это время видел пустую
 * страну при списке в три тысячи касс.
 */
class AnalyticsMapSourceTest {

    @Test
    fun `раздел открывается на источнике с готовыми координатами`() {
        val cabinet = mockCabinet(mapClient())
        val model = AnalyticsMapModel(cabinet, MapGeocoder())
        runBlocking { model.load() }

        assertEquals(emptyList(), addressesToFind(model.view), "карте пришлось искать адреса на службе карт")
        val laid = placement(model.view) { AddressAnswer.Searching }
        assertEquals(FLEET, laid.placed.size, "на карту встала не вся сеть")
        assertTrue(laid.unplaced.isEmpty(), "кассы остались вне карты: ${laid.unplaced.size}")
    }

    /**
     * Кабинет показа: координаты только у источника «по кабинету».
     *
     * Так отвечает и настоящий: у адреса торговой точки координат нет,
     * и подменять это в проверке значило бы проверять не тот ответ.
     */
    private fun mapClient(): CabinetClient {
        val engine = MockEngine { request ->
            val byCabinet = request.url.encodedQuery.contains("CABINET_COORDINATES")
            respond(
                content = if (byCabinet) WITH_POINTS else WITHOUT_POINTS,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    private companion object {
        const val FLEET = 2

        const val WITH_POINTS = """{"positionSource":"CABINET_COORDINATES","placedCount":2,
            "placed":[
              {"cashRegisterId":"c1","address":"Алматы, Медеуский, Достык, 10",
               "position":{"latitude":43.222293,"longitude":76.958049}},
              {"cashRegisterId":"c2","address":"Астана, Сарыарка, Айтматов, 77",
               "position":{"latitude":51.128207,"longitude":71.430411}}]}"""

        const val WITHOUT_POINTS = """{"positionSource":"RETAIL_PLACE_ADDRESS","placedCount":2,
            "placed":[
              {"cashRegisterId":"c1","address":"Алматы, Медеуский, Достык, 10",
               "position":{"latitude":null,"longitude":null}},
              {"cashRegisterId":"c2","address":"Астана, Сарыарка, Айтматов, 77",
               "position":{"latitude":null,"longitude":null}}]}"""
    }
}
