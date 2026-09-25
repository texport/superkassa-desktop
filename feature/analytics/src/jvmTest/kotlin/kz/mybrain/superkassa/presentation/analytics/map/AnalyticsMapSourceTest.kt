package kz.mybrain.superkassa.presentation.analytics.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.data.analytics.CabinetReplies
import kz.mybrain.superkassa.domain.analytics.model.AddressAnswer
import kz.mybrain.superkassa.domain.analytics.model.addressesToFind
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.presentation.analytics.mapModel
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
        val maps = QuietMaps()
        val model = mapModel(mapCabinet().analytics, maps)
        runBlocking {
            withContext(Dispatchers.Main) { model.follow("token") }
            model.state.first { it.reading.value != null }
        }
        val view = model.state.value.reading.value

        assertEquals(emptyList(), addressesToFind(view), "карте пришлось искать адреса на службе карт")
        assertEquals(emptyList(), maps.searched, "карта спросила службу карт")
        val laid = placement(view) { AddressAnswer.Searching }
        assertEquals(FLEET, laid.placed.size, "на карту встала не вся сеть")
        assertTrue(laid.unplaced.isEmpty(), "кассы остались вне карты: ${laid.unplaced.size}")
    }

    /**
     * Кабинет показа: координаты только у источника «по кабинету».
     *
     * Так отвечает и настоящий: у адреса торговой точки координат нет,
     * и подменять это в проверке значило бы проверять не тот ответ.
     */
    private fun mapCabinet() = CabinetReplies.answering { target ->
        if (target.contains("CABINET_COORDINATES")) WITH_POINTS else WITHOUT_POINTS
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
