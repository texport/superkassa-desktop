package kz.mybrain.superkassa.domain.map.usecase

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.map.MemoryMapMemory
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.model.SelfPlace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * «Где я»: своя служба машины — до дома, адрес подключения — только
 * с разрешения владельца и только до города.
 */
class LocateSelfTest {
    private val house = MapPlace(43.238949, 76.889709, "")
    private val city = MapPlace(43.25, 76.92, "Almaty")

    @Test
    fun `машина знает своё место — оно точное, и наружу ничего не уходит`() {
        val maps = QuietMaps(machine = house, connection = city)
        val place = runBlocking { LocateSelf(maps, MemoryMapMemory(locationAllowed = true))() }
        assertEquals(SelfPlace.Found(house, precise = true), place)
        assertEquals(0, maps.connectionAsked)
    }

    @Test
    fun `владельца о запасном пути ещё не спрашивали — спросить, а не идти наружу`() {
        val maps = QuietMaps(connection = city)
        assertEquals(SelfPlace.AskOwner, runBlocking { LocateSelf(maps, MemoryMapMemory())() })
        assertEquals(0, maps.connectionAsked)
    }

    /**
     * Запомненный отказ делал «Где я» мёртвой кнопкой навсегда: нажатие
     * молча ничего не делало, а передумать было негде. Теперь нажатие
     * после отказа снова спрашивает владельца — и наружу по-прежнему
     * ничего не уходит, пока он не разрешит.
     */
    @Test
    fun `разрешённый запасной путь даёт город, а после отказа нажатие снова спрашивает`() {
        val maps = QuietMaps(connection = city)
        val allowed = runBlocking { LocateSelf(maps, MemoryMapMemory(locationAllowed = true))() }
        assertEquals(SelfPlace.Found(city, precise = false), allowed)
        val denied = QuietMaps(connection = city)
        assertEquals(SelfPlace.AskOwner, runBlocking { LocateSelf(denied, MemoryMapMemory(locationAllowed = false))() })
        assertEquals(0, denied.connectionAsked, "после отказа адрес ушёл наружу без вопроса")
    }

    @Test
    fun `ответ владельца помнится, и разрешение сразу ищет место`() {
        val memory = MemoryMapMemory()
        val maps = QuietMaps(connection = city)
        assertEquals(SelfPlace.Found(city, precise = false), runBlocking { AnswerLocationAsk(maps, memory)(true) })
        assertEquals(true, memory.locationAllowed)

        val refused = MemoryMapMemory()
        assertEquals(SelfPlace.Unknown, runBlocking { AnswerLocationAsk(maps, refused)(false) })
        assertEquals(false, refused.locationAllowed)
        assertEquals(1, maps.connectionAsked, "после отказа адрес ушёл наружу")
    }

    @Test
    fun `служба не ответила — места нет, и это не ошибка`() {
        val place = runBlocking { LocateSelf(QuietMaps(), MemoryMapMemory(locationAllowed = true))() }
        assertEquals(SelfPlace.Unknown, place)
        assertNull((place as? SelfPlace.Found)?.place)
    }
}
