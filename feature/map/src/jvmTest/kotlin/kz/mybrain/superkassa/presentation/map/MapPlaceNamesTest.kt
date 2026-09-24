package kz.mybrain.superkassa.presentation.map

import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Названия карты сводятся с названиями регистра: без слов о виде объекта,
 * улица с улицей, дом — по точному номеру.
 */
class MapPlaceNamesTest {

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
}
