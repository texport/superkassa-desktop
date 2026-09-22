package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.ui.map.MapState
import kz.mybrain.superkassa.desktop.ui.map.MapView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Карта слушается мыши: тянется за указателем и приближается колесом.
 *
 * Проверяется через сцену, а не вызовом состояния напрямую: сдвиг
 * и колесо ловит показ, и сломаться они могут в нём — перехваченным
 * событием или обработчиком, который так и не дождался жеста.
 */
class MapGestureTest {

    private fun probe(state: MapState) = RenderProbe(WIDTH, HEIGHT) {
        Box(Modifier.fillMaxSize()) {
            MapView(state, Look.tiles(), Look.cabinet.map, Modifier.fillMaxSize())
        }
    }

    @Test
    fun `перетаскивание двигает карту вслед за указателем`() {
        val state = MapState()
        val before = state.centerLongitude to state.centerLatitude
        probe(state).use { probe ->
            probe.frame()
            probe.drag(Offset(MIDDLE_X, MIDDLE_Y), Offset(MIDDLE_X - SHIFT, MIDDLE_Y))
        }
        assertTrue(state.centerLongitude > before.first, "карту потянули влево — центр должен уйти на восток")
        assertEquals(before.second, state.centerLatitude, 1e-9)
    }

    /**
     * Карта касс пересобирает состояние, когда набор касс сменился, — тем же
     * увеличением. Показ обязан тянуть новое состояние, а не то, что было
     * при первом кадре: иначе движется карта, которой уже нет.
     */
    @Test
    fun `перетаскивание двигает подменённое состояние, а не прежнее`() {
        val first = MapState()
        val second = MapState()
        val holder = mutableStateOf(first)
        RenderProbe(WIDTH, HEIGHT) {
            Box(Modifier.fillMaxSize()) {
                MapView(holder.value, Look.tiles(), Look.cabinet.map, Modifier.fillMaxSize())
            }
        }.use { probe ->
            probe.frame()
            holder.value = second
            probe.frame()
            probe.drag(Offset(MIDDLE_X, MIDDLE_Y), Offset(MIDDLE_X - SHIFT, MIDDLE_Y))
        }
        assertEquals(Look.LONGITUDE, first.centerLongitude, 1e-9, "прежнее состояние трогать нельзя")
        assertTrue(second.centerLongitude > Look.LONGITUDE, "новое состояние не сдвинулось")
    }

    @Test
    fun `колесо от себя приближает, к себе отдаляет`() {
        val state = MapState()
        val zoom = state.zoom
        probe(state).use { probe ->
            probe.frame()
            probe.wheel(Offset(MIDDLE_X, MIDDLE_Y), ticks = -1f)
            assertEquals(zoom + 1, state.zoom, "колесо от себя не приблизило")
            probe.wheel(Offset(MIDDLE_X, MIDDLE_Y), ticks = 2f)
            assertEquals(zoom - 1, state.zoom, "колесо к себе не отдалило")
        }
    }

    @Test
    fun `колесо приближает к указателю, а не к середине`() {
        val state = MapState()
        val before = state.centerLongitude
        probe(state).use { probe ->
            probe.frame()
            probe.wheel(Offset(MIDDLE_X + SHIFT, MIDDLE_Y), ticks = -1f)
        }
        assertNotEquals(before, state.centerLongitude, "центр не сдвинулся к указателю")
        assertTrue(state.centerLongitude > before, "приближение к точке справа уводит центр на восток")
    }

    private companion object {
        const val WIDTH = 800
        const val HEIGHT = 600
        const val MIDDLE_X = 400f
        const val MIDDLE_Y = 300f
        const val SHIFT = 150f
    }
}
