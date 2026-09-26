package kz.mybrain.superkassa.presentation.common.mapview.grid

import kz.mybrain.superkassa.domain.map.model.TileGrid
import kz.mybrain.superkassa.presentation.common.mapview.MapProjection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Строки плиток Яндекса в сферической сетке карты.
 *
 * Для Алматы (43,25° с. ш.) на увеличении 12 OpenStreetMap отдаёт строку
 * 1501, а Яндекс ту же местность — строкой 1504: его сетка эллиптическая.
 * Плитка, поставленная по номеру, ушла бы на три строки — десятки километров.
 */
class TileRowsTest {
    private val almaty = MapProjection.yOf(ALMATY, ZOOM)

    @Test
    fun `сферическая сетка — номер строки`() {
        assertEquals(1501, TileRows.at(TileGrid.WebMercator, ZOOM, almaty))
        assertEquals(1501.0 * MapProjection.TILE, TileRows.top(TileGrid.WebMercator, ZOOM, 1501))
    }

    @Test
    fun `эллиптическая сетка Яндекса ставит ту же местность в свою строку`() {
        assertEquals(1504, TileRows.at(TileGrid.EllipticalMercator, ZOOM, almaty))
        val top = TileRows.top(TileGrid.EllipticalMercator, ZOOM, 1504)
        val bottom = TileRows.top(TileGrid.EllipticalMercator, ZOOM, 1505)
        assertTrue(almaty in top..bottom, "Алматы вне своей плитки Яндекса: $top..$bottom, $almaty")
        assertTrue(abs(bottom - top - MapProjection.TILE) < 1.0, "строка Яндекса почти 256 точек: ${bottom - top}")
    }

    @Test
    fun `широта и эллиптическая точка обратимы`() {
        listOf(-60.0, 0.0, 43.25, 51.17, 80.0).forEach { latitude ->
            val y = TileRows.ellipticalY(latitude, ZOOM)
            assertTrue(abs(TileRows.ellipticalLatitude(y, ZOOM) - latitude) < 1e-7, "широта $latitude")
        }
    }

    private companion object {
        const val ALMATY = 43.25
        const val ZOOM = 12
    }
}
