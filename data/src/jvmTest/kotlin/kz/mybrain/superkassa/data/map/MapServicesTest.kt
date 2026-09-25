package kz.mybrain.superkassa.data.map

import kotlinx.io.files.Path
import kz.mybrain.superkassa.data.local.workplace.Preferences
import kz.mybrain.superkassa.integrations.maps.MapServices
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
        return Preferences(Path(directory.path))
    }

    @Test
    fun `без настройки работают общедоступные службы`() {
        val services = MapAddresses(preferences().maps).services()

        assertEquals(MapServices.TILES, services.tiles)
        assertEquals(MapServices.SEARCH, services.search)
        assertEquals(MapServices.REVERSE, services.reverse)
        assertEquals(MapServices.LOCATION, services.location)
    }

    @Test
    fun `заданный адрес доходит до службы`() {
        val preferences = preferences()
        preferences.maps.tiles = "https://tiles.bfd.kz"
        preferences.maps.search = "https://search.bfd.kz/find"
        preferences.maps.reverse = "https://search.bfd.kz/reverse"
        preferences.maps.location = "https://where.bfd.kz/json"

        val services = MapAddresses(preferences.maps).services()

        assertEquals("https://tiles.bfd.kz", services.tiles)
        assertEquals("https://search.bfd.kz/find", services.search)
        assertEquals("https://search.bfd.kz/reverse", services.reverse)
        assertEquals("https://where.bfd.kz/json", services.location)
    }

    @Test
    fun `пустой адрес возвращает общедоступную службу`() {
        val preferences = preferences()
        preferences.maps.tiles = "https://tiles.bfd.kz"
        preferences.maps.tiles = null

        assertEquals(MapServices.TILES, MapAddresses(preferences.maps).services().tiles)
    }
}
