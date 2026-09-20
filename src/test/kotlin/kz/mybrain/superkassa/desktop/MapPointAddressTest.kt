package kz.mybrain.superkassa.desktop

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.server.cabinet.AddressSuggestion
import kz.mybrain.superkassa.desktop.ui.map.MapPointPlace
import kz.mybrain.superkassa.desktop.ui.map.PointMatch
import kz.mybrain.superkassa.desktop.ui.map.PointStep
import kz.mybrain.superkassa.desktop.ui.map.RegistrySteps
import kz.mybrain.superkassa.desktop.ui.map.addressNeedle
import kz.mybrain.superkassa.desktop.ui.map.matchHouses
import kz.mybrain.superkassa.desktop.ui.map.matchPointPlace
import kz.mybrain.superkassa.desktop.ui.map.matchSuggestion
import kz.mybrain.superkassa.desktop.ui.map.pointPlaceOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Подбор адреса по метке на карте.
 *
 * Метку ставит владелец, место называет служба карт, а адресом точки
 * становится только запись государственного регистра: РКА и САТО есть
 * лишь у него. Проверяется разбор ответа службы и сведение названного
 * ею места с шагами регистра.
 */
class MapPointAddressTest {

    /**
     * Ответ службы обратного геокодирования.
     *
     * Форма снята с ответа Nominatim `reverse` по точке в Алматы: пункт
     * приходит сразу под несколькими именами — `city` и `suburb`, —
     * и какое из них есть в регистре, заранее неизвестно.
     */
    private val almaty = """
        {"lat":"43.238949","lon":"76.889709",
         "display_name":"10, проспект Достык, Медеуский район, Алматы, 050000, Казахстан",
         "address":{"house_number":"10","road":"проспект Достык","suburb":"Медеуский район",
                    "city":"Алматы","state":"Алматы","postcode":"050000",
                    "country":"Казахстан","country_code":"kz"}}
    """.trimIndent()

    @Test
    fun `ответ службы разбирается до шагов регистра`() {
        val place = pointPlaceOf(almaty)

        assertEquals("Алматы", place?.region)
        assertEquals(listOf("Алматы", "Медеуский район"), place?.localities)
        assertEquals("проспект Достык", place?.street)
        assertEquals("10", place?.house)
    }

    @Test
    fun `точка без адреса местом не считается`() {
        assertNull(pointPlaceOf("""{"lat":"48.0","lon":"67.0","display_name":"Казахстан"}"""))
        assertNull(pointPlaceOf("не ответ службы вовсе"))
    }

    @Test
    fun `место без улицы и дома всё равно разбирается`() {
        val place = pointPlaceOf("""{"address":{"state":"Абайская область","town":"Семей"}}""")

        assertEquals("Абайская область", place?.region)
        assertEquals(listOf("Семей"), place?.localities)
        assertEquals("", place?.street)
        assertEquals("", place?.house)
    }

    @Test
    fun `регистр спрашивается без слов о виде объекта`() {
        assertEquals("Достык", addressNeedle("проспект Достык"))
        assertEquals("Медеуский", addressNeedle("Медеуский район"))
        assertEquals("Абайская", addressNeedle("Абайская область"))
        assertEquals("Алматы", addressNeedle("Алматы"))
    }

    @Test
    fun `улица карты сводится с улицей регистра`() {
        val streets = listOf(
            AddressSuggestion(id = 1, name = "Абая даңғылы", level = "STREET"),
            AddressSuggestion(id = 2, name = "Достык даңғылы", level = "STREET")
        )

        assertEquals(2L, matchSuggestion(streets, "проспект Достык")?.id)
    }

    @Test
    fun `чужого названия в регистре не находится`() {
        val streets = listOf(AddressSuggestion(id = 1, name = "Абая даңғылы", level = "STREET"))

        assertNull(matchSuggestion(streets, "проспект Достык"))
        assertNull(matchSuggestion(streets, ""))
    }

    @Test
    fun `дома отбираются по точному номеру, а не по вхождению`() {
        val houses = listOf(
            AddressSuggestion(id = 1, name = "10", rka = "0201300118176503", level = "BUILDING"),
            AddressSuggestion(id = 2, name = "10", rka = "2201300111910697", level = "BUILDING"),
            AddressSuggestion(id = 3, name = "101", rka = "0201300116990906", level = "BUILDING")
        )

        val found = matchHouses(houses, "10")

        assertEquals(listOf(1L, 2L), found.map { it.id })
        assertTrue(found.all { it.rka != null })
    }

    @Test
    fun `место под меткой доводится до домов регистра`() {
        val place = pointPlaceOf(almaty)!!

        val outcome = runBlocking { matchPointPlace(registry, place) }

        val houses = outcome as PointMatch.Houses
        assertEquals(listOf("0201300118176503", "2201300111910697"), houses.items.mapNotNull { it.rka })
    }

    @Test
    fun `улица района ищется под районом, а не под городом`() {
        val place = MapPointPlace("Алматы", listOf("Алматы", "Медеуский район"), "улица Пушкина", "1")

        val outcome = runBlocking { matchPointPlace(registry, place) }

        assertEquals(PointMatch.Houses(listOf(pushkina1)), outcome)
    }

    @Test
    fun `не найденное в регистре названо шагом, на котором подбор встал`() {
        val cases = mapOf(
            PointStep.Region to MapPointPlace("Тверская область", listOf("Тверь"), "Мира", "1"),
            PointStep.Locality to MapPointPlace("Алматы", listOf("Тверь"), "Мира", "1"),
            PointStep.Street to MapPointPlace("Алматы", listOf("Алматы"), "улица Мира", "1"),
            PointStep.House to MapPointPlace("Алматы", listOf("Алматы"), "проспект Достык", "7")
        )

        cases.forEach { (step, place) ->
            assertEquals(PointMatch.Missing(step), runBlocking { matchPointPlace(registry, place) }, "$step")
        }
    }

    @Test
    fun `место без дома до адреса не доводится`() {
        val place = MapPointPlace("Алматы", listOf("Алматы"), "проспект Достык", "")

        assertEquals(PointMatch.Missing(PointStep.House), runBlocking { matchPointPlace(registry, place) })
    }

    private val pushkina1 =
        AddressSuggestion(id = 71, name = "1", rka = "0201300110000011", level = "BUILDING")

    /**
     * Адресный регистр стенда в четырёх шагах.
     *
     * Записи повторяют то, что отдаёт кабинет: у Алматы под городом лежит
     * район, улицы города и района разные, а на номер дома приходит по две
     * записи с разными кодами РКА. Улица города находится и тогда, когда
     * метка попала в район: подбор спускается до района, а улицу ищет
     * снизу вверх.
     */
    private val registry = object : RegistrySteps {
        override suspend fun regions(query: String) = found(
            query,
            listOf(AddressSuggestion(id = 2, name = "Алматы", level = "REGION"))
        )

        override suspend fun localities(parentId: Long, query: String) = found(
            query,
            when (parentId) {
                2L -> listOf(AddressSuggestion(id = 20, name = "Алматы", level = "LOCALITY"))
                20L -> listOf(AddressSuggestion(id = 21, name = "Медеуский район", level = "LOCALITY"))
                else -> emptyList()
            }
        )

        override suspend fun streets(localityId: Long, query: String) = found(
            query,
            when (localityId) {
                20L -> listOf(AddressSuggestion(id = 143546, name = "Достык даңғылы", level = "STREET"))
                21L -> listOf(AddressSuggestion(id = 70, name = "Пушкина көшесі", level = "STREET"))
                else -> emptyList()
            }
        )

        override suspend fun buildings(streetId: Long, number: String) = found(
            number,
            when (streetId) {
                143546L -> listOf(
                    AddressSuggestion(id = 1, name = "10", rka = "0201300118176503", level = "BUILDING"),
                    AddressSuggestion(id = 2, name = "10", rka = "2201300111910697", level = "BUILDING"),
                    AddressSuggestion(id = 3, name = "101", rka = "0201300116990906", level = "BUILDING")
                )

                70L -> listOf(pushkina1)
                else -> emptyList()
            }
        )

        /** Регистр ищет по вхождению и регистра букв не различает. */
        private fun found(query: String, items: List<AddressSuggestion>): List<AddressSuggestion> =
            items.filter { it.name.lowercase().contains(query.lowercase()) }
    }
}
