package kz.mybrain.superkassa.domain.map.usecase

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.map.MemoryMapMemory
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.model.MapPanel
import kz.mybrain.superkassa.domain.map.model.MapPanels
import kz.mybrain.superkassa.domain.map.model.MapPlace
import kotlin.test.Test
import kotlin.test.assertEquals

/** Дома торговых точек и свёрнутые части карты. */
class MapCasesTest {

    @Test
    fun `найденный дом снова не спрашивается, а ненайденный и неотвеченный — спрашиваются`() {
        val almaty = MapPlace(43.2, 76.9, "Алматы")
        val maps = QuietMaps(found = mapOf(ALMATY to almaty), silent = setOf(URALSK))
        val answers = mutableMapOf<String, MapPlace?>()

        runBlocking {
            FindHouses(maps)(listOf(ALMATY, URALSK, ASTANA), found = setOf(ASTANA)) { address, place ->
                answers[address] = place
            }
        }

        assertEquals(listOf(ALMATY, URALSK), maps.searched)
        assertEquals(mapOf(ALMATY to almaty, URALSK to null), answers)
    }

    @Test
    fun `свёрнутая часть помнится рабочим местом по отдельности`() {
        val memory = MemoryMapMemory()
        assertEquals(MapPanels(), ReadMapPanels(memory)())

        FoldMapPanel(memory)(MapPanel.Legend, collapsed = true)
        assertEquals(MapPanels(cardCollapsed = false, legendCollapsed = true), ReadMapPanels(memory)())

        FoldMapPanel(memory)(MapPanel.Card, collapsed = true)
        FoldMapPanel(memory)(MapPanel.Legend, collapsed = false)
        assertEquals(MapPanels(cardCollapsed = true, legendCollapsed = false), ReadMapPanels(memory)())
    }

    private companion object {
        const val ALMATY = "Алматы, Медеуский, Достык, 10"
        const val URALSK = "Западно-Казахстанская, Уральск, Сарайшык, 5"
        const val ASTANA = "Астана, Есильский, Кунаева, 12"
    }
}
