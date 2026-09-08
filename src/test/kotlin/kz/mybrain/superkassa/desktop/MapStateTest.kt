package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import kz.mybrain.superkassa.desktop.ui.map.MapState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Поведение карты при выборе точки.
 *
 * Проверяется то, из-за чего в заявление ушли бы чужие координаты:
 * пределы широты и долготы, разделение «показать место» и «выбрать точку»,
 * и то, что перевод карты в найденный город метку не ставит.
 */
class MapStateTest {

    @Test
    fun `карта открывается без выбранной точки`() {
        val state = MapState()
        assertFalse(state.marked)
    }

    @Test
    fun `нажатие ставит точку там, где нажали`() {
        val state = MapState()
        state.mark(43.238949, 76.889709)
        assertTrue(state.marked)
        assertEquals(43.238949, state.markerLatitude!!, 1e-9)
        assertEquals(76.889709, state.markerLongitude!!, 1e-9)
    }

    @Test
    fun `точка за краем мира прижимается к нему, а не уходит за него`() {
        val state = MapState()
        state.mark(95.0, 200.0)
        assertEquals(MapProjection.MAX_LATITUDE, state.markerLatitude!!, 1e-9)
        assertEquals(180.0, state.markerLongitude!!, 1e-9)
    }

    @Test
    fun `показ известной точки двигает и метку, и центр`() {
        val state = MapState()
        state.show(51.180100, 71.446000)
        assertEquals(51.1801, state.centerLatitude, 1e-6)
        assertEquals(71.446, state.centerLongitude, 1e-6)
        assertTrue(state.marked)
    }

    @Test
    fun `своё место отмечается своим знаком, а не выбранной точкой`() {
        val state = MapState()
        state.showLocation(51.1801, 71.446, "Астана", 12)
        assertEquals(51.1801, state.centerLatitude, 1e-6)
        assertEquals(51.1801, state.locationLatitude!!, 1e-6)
        assertEquals("Астана", state.locationCity)
        assertEquals(12, state.zoom)
        assertTrue(state.located)
        assertFalse(state.marked, "место определено до города — выбранной точкой это не является")
    }

    @Test
    fun `выбор точки не стирает своё место`() {
        val state = MapState()
        state.showLocation(51.1801, 71.446, "Астана", 12)
        state.mark(51.15, 71.40)
        assertTrue(state.located, "синий кружок города остаётся на карте рядом с выбранной точкой")
        assertTrue(state.marked)
    }

    @Test
    fun `увеличение не выходит за пределы карты`() {
        val state = MapState()
        repeat(30) { state.zoomBy(1) }
        assertEquals(18, state.zoom)
        repeat(40) { state.zoomBy(-1) }
        assertEquals(3, state.zoom)
    }

    @Test
    fun `сдвиг карты не меняет выбранную точку`() {
        val state = MapState()
        state.mark(43.238949, 76.889709)
        state.pan(120f, -80f)
        assertEquals(43.238949, state.markerLatitude!!, 1e-9)
        assertEquals(76.889709, state.markerLongitude!!, 1e-9)
    }
}
