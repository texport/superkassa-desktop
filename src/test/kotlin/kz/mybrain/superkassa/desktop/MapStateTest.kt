package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import kz.mybrain.superkassa.desktop.ui.map.MapState
import kz.mybrain.superkassa.desktop.ui.map.MapWheel
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

    /**
     * Переход к кассе объявляется целью, а не мгновенной перестановкой центра.
     *
     * Иначе карта прыгала бы от Уральска к Алматы за один кадр, и владелец
     * терял бы, откуда она приехала.
     */
    @Test
    fun `выбор кассы объявляет цель, а не переставляет карту`() {
        val state = MapState()
        val was = state.centerLatitude

        state.glideTo(51.1801, 71.446)

        assertEquals(51.1801, state.goal!!.latitude, 1e-6)
        assertEquals(was, state.centerLatitude, 1e-9, "карта переставилась вместо перехода")
    }

    /** Рука владельца отменяет начатый переход: карта не уезжает из-под пальца. */
    @Test
    fun `перетаскивание отменяет начатый переход`() {
        val state = MapState()
        state.glideTo(51.1801, 71.446)

        state.pan(10f, 10f)

        assertEquals(null, state.goal)
    }

    /** Дойдя до цели, карта снимает её: переход не повторяется на следующем кадре. */
    @Test
    fun `по прибытии цель снимается`() {
        val state = MapState()
        state.glideTo(51.1801, 71.446)

        state.arrived()

        assertEquals(null, state.goal)
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

    @Test
    fun `приближение к указателю оставляет точку под ним на месте`() {
        val state = MapState()
        val dx = 150.0
        val dy = -80.0
        // Точка, стоявшая на 150 правее и 80 выше середины окна.
        val pointX = MapProjection.xOf(state.centerLongitude, state.zoom) + dx
        val pointY = MapProjection.yOf(state.centerLatitude, state.zoom) + dy
        val latitude = MapProjection.latitudeOf(pointY, state.zoom)
        val longitude = MapProjection.longitudeOf(pointX, state.zoom)
        state.zoomAt(latitude, longitude, dx, dy, 2)
        assertEquals(14, state.zoom)
        val afterX = MapProjection.xOf(longitude, state.zoom) - MapProjection.xOf(state.centerLongitude, state.zoom)
        val afterY = MapProjection.yOf(latitude, state.zoom) - MapProjection.yOf(state.centerLatitude, state.zoom)
        assertEquals(dx, afterX, 1e-6, "точка уехала по горизонтали")
        assertEquals(dy, afterY, 1e-6, "точка уехала по вертикали")
    }

    @Test
    fun `приближение у предела не двигает карту`() {
        val state = MapState()
        repeat(10) { state.zoomBy(1) }
        val longitude = state.centerLongitude
        state.zoomAt(43.0, 77.0, 100.0, 100.0, 1)
        assertEquals(18, state.zoom)
        assertEquals(longitude, state.centerLongitude, 1e-9, "у предела шага нет — и сдвига быть не должно")
    }

    @Test
    fun `доли хода колеса копятся до целого шага`() {
        val wheel = MapWheel()
        assertEquals(0, wheel.turn(-0.5f))
        assertEquals(1, wheel.turn(-0.5f), "две доли по половине — один шаг приближения")
        assertEquals(0, wheel.turn(-0.25f))
        assertEquals(-2, wheel.turn(2.25f), "щелчок к себе отдаляет; остаток долей не пропадает")
    }
}
