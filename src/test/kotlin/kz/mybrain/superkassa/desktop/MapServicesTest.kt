package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.map.MapService
import kz.mybrain.superkassa.desktop.ui.map.MapServices
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Службы карты обязаны заменяться настройкой, а не перевыпуском.
 *
 * Плитки и поиск адреса по умолчанию берутся у сообщества, и его правила
 * запрещают массовую выкачку: на всех владельцев выпускать приложение
 * с ними нельзя. Проверяется, что заданный адрес доходит до самой службы,
 * а пустой возвращает общедоступную.
 */
class MapServicesTest {

    /**
     * Настройки в своём каталоге.
     *
     * Каждая настройка рабочего места — отдельный файл рядом с основным,
     * и общий временный каталог сделал бы наборы разных проверок одним:
     * заданный в одной адрес доходил до другой.
     */
    private fun preferences(): Preferences {
        val directory = Files.createTempDirectory("map").toFile().also { it.deleteOnExit() }
        return Preferences(File(directory, "kkm"))
    }

    @Test
    fun `без настройки работают общедоступные службы`() {
        val services = MapServices(preferences())

        assertEquals(MapService.TILES, services.tiles.source)
        assertEquals(MapService.SEARCH, services.geocoder.service)
        assertEquals(MapService.REVERSE, services.reverse.service)
        assertEquals(MapService.LOCATION, services.locator.service)
    }

    @Test
    fun `заданный адрес доходит до службы`() {
        val preferences = preferences()
        preferences.maps.tiles = "https://tiles.bfd.kz"
        preferences.maps.search = "https://search.bfd.kz/find"
        preferences.maps.reverse = "https://search.bfd.kz/reverse"
        preferences.maps.location = "https://where.bfd.kz/json"

        val services = MapServices(preferences)

        assertEquals("https://tiles.bfd.kz", services.tiles.source)
        assertEquals("https://search.bfd.kz/find", services.geocoder.service)
        assertEquals("https://search.bfd.kz/reverse", services.reverse.service)
        assertEquals("https://where.bfd.kz/json", services.locator.service)
    }

    @Test
    fun `пустой адрес возвращает общедоступную службу`() {
        val preferences = preferences()
        preferences.maps.tiles = "https://tiles.bfd.kz"
        preferences.maps.tiles = null

        assertEquals(MapService.TILES, MapServices(preferences).tiles.source)
    }
}
