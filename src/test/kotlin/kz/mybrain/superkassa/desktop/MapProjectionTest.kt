package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проекция карты.
 *
 * Ошибка здесь не видна глазом и не ломает сборку: карта просто ставит
 * точку не туда, а в заявление уходят координаты чужого квартала.
 * Поэтому проверяются известные значения, а не поведение показа.
 */
class MapProjectionTest {

    @Test
    fun `нулевой меридиан и экватор попадают в середину мира`() {
        assertEquals(128.0, MapProjection.xOf(0.0, 0), 1e-6)
        assertEquals(128.0, MapProjection.yOf(0.0, 0), 1e-6)
    }

    @Test
    fun `края мира — это края полотна`() {
        assertEquals(0.0, MapProjection.xOf(-180.0, 0), 1e-6)
        assertEquals(256.0, MapProjection.xOf(180.0, 0), 1e-6)
        assertEquals(0.0, MapProjection.yOf(MapProjection.MAX_LATITUDE, 0), 1e-3)
        assertEquals(256.0, MapProjection.yOf(-MapProjection.MAX_LATITUDE, 0), 1e-3)
    }

    @Test
    fun `градусы возвращаются теми же после перевода в точки и обратно`() {
        val latitude = 43.238949
        val longitude = 76.889709
        for (zoom in 3..18) {
            val x = MapProjection.xOf(longitude, zoom)
            val y = MapProjection.yOf(latitude, zoom)
            assertTrue(abs(MapProjection.longitudeOf(x, zoom) - longitude) < 1e-6, "долгота на $zoom")
            assertTrue(abs(MapProjection.latitudeOf(y, zoom) - latitude) < 1e-6, "широта на $zoom")
        }
    }

    @Test
    fun `увеличение удваивает число плиток по стороне`() {
        assertEquals(1, MapProjection.tiles(0))
        assertEquals(2, MapProjection.tiles(1))
        assertEquals(4096, MapProjection.tiles(12))
    }

    @Test
    fun `западное и восточное полушария лежат в разных плитках`() {
        assertEquals(0, MapProjection.tileOf(MapProjection.xOf(-90.0, 1)))
        assertEquals(1, MapProjection.tileOf(MapProjection.xOf(90.0, 1)))
    }

    @Test
    fun `широта за пределом проекции прижимается к краю, а не уходит в бесконечность`() {
        val beyond = MapProjection.yOf(89.9, 0)
        assertTrue(beyond.isFinite(), "за пределом проекции получилось не число")
        assertEquals(MapProjection.yOf(MapProjection.MAX_LATITUDE, 0), beyond, 1e-9)
    }

    @Test
    fun `координаты уходят в кабинет с шестью знаками, а не со всеми`() {
        assertEquals("43.238949", MapProjection.degrees(43.2389493827).toPlainString())
        assertEquals("76.8", MapProjection.degrees(76.8000000).toPlainString())
    }
}
