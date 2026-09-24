package kz.mybrain.superkassa.integrations.maps

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ответы Nominatim и службы адреса подключения разбираются в места.
 *
 * Тела сняты с настоящих ответов служб; нужное в них — координаты строками
 * и разобранный адрес под несколькими именами пункта.
 */
class MapRepliesTest {

    /** Ответ `reverse` по точке в Алматы: пункт приходит и `city`, и `suburb`. */
    private val almaty = """
        {"lat":"43.238949","lon":"76.889709",
         "display_name":"10, проспект Достык, Медеуский район, Алматы, 050000, Казахстан",
         "address":{"house_number":"10","road":"проспект Достык","suburb":"Медеуский район",
                    "city":"Алматы","state":"Алматы","postcode":"050000",
                    "country":"Казахстан","country_code":"kz"}}
    """.trimIndent()

    @Test
    fun reverseAnswerSplitsIntoRegistrySteps() = runTest {
        val fake = MapsFake.json(almaty)
        val place = OpenMaps(engine = fake.engine).placeAt(43.238949, 76.889709, "ru")

        assertEquals("Алматы", place?.region)
        assertEquals(listOf("Алматы", "Медеуский район"), place?.localities)
        assertEquals("проспект Достык", place?.street)
        assertEquals("10", place?.house)
        val url = fake.asked.single().url
        assertEquals("43.238949", url.parameters["lat"])
        assertEquals("ru", url.parameters["accept-language"])
    }

    /** Точка в степи: служба отвечает, но адреса нет — места нет, и это не сбой. */
    @Test
    fun pointWithoutAddressIsNoPlace() = runTest {
        val steppe = """{"lat":"48.0","lon":"67.0","display_name":"Казахстан"}"""
        val empty = OpenMaps(engine = MapsFake.json(steppe).engine)
        val garbage = OpenMaps(engine = MapsFake.json("не ответ службы вовсе").engine)

        assertNull(empty.placeAt(48.0, 67.0, "ru"))
        assertNull(garbage.placeAt(48.0, 67.0, "ru"))
    }

    /** Без улицы и дома место разбирается: область и пункт регистр доведёт до улицы. */
    @Test
    fun placeWithoutStreetIsStillPlace() = runTest {
        val semey = """{"address":{"state":"Абайская область","town":"Семей"}}"""
        val maps = OpenMaps(engine = MapsFake.json(semey).engine)
        val place = maps.placeAt(50.4, 80.2, "ru")

        assertEquals("Абайская область", place?.region)
        assertEquals(listOf("Семей"), place?.localities)
        assertEquals("", place?.street)
        assertEquals("", place?.house)
    }

    @Test
    fun searchAnswerGivesPlacesAndSkipsBrokenOnes() = runTest {
        val body = """[{"lat":"43.2389","lon":"76.8897","display_name":"Абая, 10, Алматы"},
                       {"lat":"не число","lon":"76.0","display_name":"сломанная запись"}]"""
        val found = OpenMaps(engine = MapsFake.json(body).engine).find("Абая 10", "ru")

        assertEquals(listOf(MapPlace(43.2389, 76.8897, "Абая, 10, Алматы")), found)
    }

    @Test
    fun connectionAddressGivesCity() = runTest {
        val body = """{"ip":"212.154.10.7","city":"Almaty","loc":"43.2567,76.9286","country":"KZ"}"""
        val place = OpenMaps(engine = MapsFake.json(body).engine).locateByConnection()

        assertEquals(MapPlace(43.2567, 76.9286, "Almaty"), place)
    }

    @Test
    fun connectionAnswerWithoutCoordinatesIsNoPlace() = runTest {
        assertNull(OpenMaps(engine = MapsFake.json("""{"city":"Almaty","loc":"где-то"}""").engine).locateByConnection())
        assertTrue(OpenMaps(engine = MapsFake.json("[]").engine).find("   ", "ru")!!.isEmpty())
    }
}
