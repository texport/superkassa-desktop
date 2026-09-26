package kz.mybrain.superkassa.data.map

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.integrations.maps.MapServices
import kz.mybrain.superkassa.integrations.maps.MapTile
import kz.mybrain.superkassa.integrations.maps.OpenMaps
import kz.mybrain.superkassa.integrations.maps.TileProviders
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Службы карт через модуль: язык владельца, перевод мест, плитки на диске и осечки. */
class OpenStreetMapsTest {

    @Test
    fun `поиск идёт на языке владельца, а найденное названо словами службы`() {
        var asked = ""
        val engine = MockEngine { request ->
            asked = request.url.parameters["accept-language"].orEmpty()
            val found = """[{"lat":"43.2389","lon":"76.8897","display_name":"Абая, 10, Алматы"}]"""
            respond(found, HttpStatusCode.OK, JSON)
        }
        val maps = OpenStreetMaps(OpenMaps(engine = engine), language = { "kk" })

        assertEquals(listOf(MapPlace(43.2389, 76.8897, "Абая, 10, Алматы")), runBlocking { maps.find("Абая, 10") })
        assertEquals("kk", asked)
    }

    /**
     * Адрес службы владелец задаёт сам, и задать можно и такой, по которому
     * сеть не ходит: это «служба не ответила», а не упавший раздел.
     */
    @Test
    fun `служба, по адресу которой сеть не ходит, — молчание, а не сбой`() {
        val services = MapServices(provider = TileProviders.custom("file:///no-tiles"), search = "file:///no-search")
        val maps = OpenStreetMaps(OpenMaps(services = { services }), language = { "ru" })

        assertNull(runBlocking { maps.tile(12, 1, 1) })
        assertNull(runBlocking { maps.find("Абая, 10") })
    }

    @Test
    fun `плитка, раз положенная на диск, читается оттуда`() {
        val folder = Files.createTempDirectory("tiles").toFile().also { it.deleteOnExit() }
        val tiles = DiskTiles(folder.path)
        val tile = MapTile(12, 2345, 1456)

        assertNull(runBlocking { tiles.read(OSM, tile) })
        runBlocking { tiles.write(OSM, tile, PNG) }

        assertContentEquals(PNG, runBlocking { tiles.read(OSM, tile) })
        val reopened = runBlocking { DiskTiles(folder.path).read(OSM, tile) }
        assertContentEquals(PNG, reopened, "плитка не пережила закрытие карты")
        assertNull(runBlocking { tiles.read("2gis", tile) }, "плитка OSM показана картой 2ГИС")
    }

    private companion object {
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        val PNG = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        const val OSM = "osm"
    }
}
