package kz.mybrain.superkassa.integrations.maps

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Правила обращения к службам OSM: имя приложения, пауза, память найденного
 * и хранение плиток.
 *
 * Правила эти — условие пользования общедоступными службами; нарушившее их
 * приложение служба блокирует для всех касс разом.
 */
class OpenMapsTest {

    private val found = """[{"lat":"43.2389","lon":"76.8897","display_name":"Абая, 10, Алматы"}]"""

    @Test
    fun everyRequestNamesTheApplication() = runTest {
        val fake = MapsFake.json(found)
        OpenMaps(engine = fake.engine).find("Абая 10", "ru")

        assertEquals(MapServices.USER_AGENT, fake.asked.single().headers[HttpHeaders.UserAgent])
        assertEquals("kz", fake.asked.single().url.parameters["countrycodes"])
        assertEquals("Абая 10", fake.asked.single().url.parameters["q"])
    }

    @Test
    fun foundAddressIsAskedOnce() = runTest {
        val fake = MapsFake.json(found)
        val maps = OpenMaps(engine = fake.engine)

        maps.find("Абая 10", "ru")
        maps.find(" Абая 10 ", "ru")

        assertEquals(1, fake.asked.size)
    }

    /** Не ответившая служба не делает адрес ненайденным: следующий вопрос уходит в сеть. */
    @Test
    fun silentServiceIsNotRemembered() = runTest {
        var calls = 0
        val fake = MapsFake {
            calls++
            if (calls == 1) throw IOException("Network is unreachable")
            respond(found, HttpStatusCode.OK, MapsFake.jsonHeaders)
        }
        val failures = mutableListOf<String>()
        val maps = OpenMaps(engine = fake.engine, journal = { service, reason -> failures += "$service $reason" })

        assertNull(maps.find("Абая 10", "ru"))
        assertEquals(1, maps.find("Абая 10", "ru")?.size)
        assertEquals(listOf("search IOException"), failures)
    }

    @Test
    fun searchesKeepThePauseOfTheService() = runTest {
        val fake = MapsFake.json(found)
        val clock = testScheduler.timeSource
        val maps = OpenMaps(engine = fake.engine, clock = clock)
        val start = clock.markNow()

        maps.find("Абая 10", "ru")
        maps.find("Достык 5", "ru")

        assertEquals(2, fake.asked.size)
        assertTrue(start.elapsedNow() >= MapServices.SEARCH_PAUSE)
    }

    @Test
    fun configuredAddressReachesTheService() = runTest {
        val fake = MapsFake.json(found)
        val own = MapServices(search = "https://search.bfd.kz/find", searchPause = 0.milliseconds)
        OpenMaps(services = { own }, engine = fake.engine).find("Абая 10", "kk")

        assertEquals("search.bfd.kz", fake.asked.single().url.host)
        assertEquals("/find", fake.asked.single().url.encodedPath)
    }

    @Test
    fun tileIsStoredAndNotAskedAgain() = runTest {
        val png = byteArrayOf(-119, 80, 78, 71)
        val fake = MapsFake { respond(png, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/png")) }
        val store = MemoryTiles()
        val maps = OpenMaps(tiles = store, engine = fake.engine)

        assertContentEquals(png, maps.tile(MapTile(12, 2871, 1478)))
        assertContentEquals(png, maps.tile(MapTile(12, 2871, 1478)))
        assertEquals(1, fake.asked.size)
        assertEquals("/12/2871/1478.png", fake.asked.single().url.encodedPath)
    }

    /** Страница ошибки прокси с кодом 200 — не плитка: её не показывают и не хранят. */
    @Test
    fun pageInsteadOfImageIsNoTile() = runTest {
        val page = headersOf(HttpHeaders.ContentType, "text/html")
        val fake = MapsFake { respond("<html>blocked</html>", HttpStatusCode.OK, page) }
        val store = MemoryTiles()

        assertNull(OpenMaps(tiles = store, engine = fake.engine).tile(MapTile(1, 0, 0)))
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun refusedTileIsNoTile() = runTest {
        val fake = MapsFake { respond("", HttpStatusCode.Forbidden) }

        assertNull(OpenMaps(engine = fake.engine).tile(MapTile(1, 0, 0)))
    }

    /**
     * Адрес, которого движок не принимает, — та же неответившая служба.
     *
     * Такой адрес владелец может вписать в настройках, и проверки окна
     * ставят его, чтобы не ходить в сеть: карта должна остаться пустой,
     * а не уронить раздел исключением.
     */
    @Test
    fun unacceptableAddressIsSilence() = runTest {
        val nowhere = "file:///superkassa-no-service"
        val failures = mutableListOf<String>()
        val services = MapServices(tiles = nowhere, search = nowhere, location = nowhere, searchPause = 0.milliseconds)
        val maps = OpenMaps(services = { services }, journal = { service, _ -> failures += service })

        assertNull(maps.tile(MapTile(1, 0, 0)))
        assertNull(maps.find("Абая 10", "ru"))
        assertNull(maps.locateByConnection())
        assertEquals(listOf("tiles", "search", "location"), failures)
    }

    private class MemoryTiles : TileStore {
        val saved = mutableMapOf<MapTile, ByteArray>()

        override suspend fun read(tile: MapTile): ByteArray? = saved[tile]

        override suspend fun write(tile: MapTile, image: ByteArray) {
            saved[tile] = image
        }
    }
}
