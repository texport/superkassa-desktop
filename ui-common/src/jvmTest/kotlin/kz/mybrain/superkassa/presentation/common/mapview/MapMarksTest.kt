package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.RenderProbe
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ярлычок едет вместе с картой.
 *
 * Место ярлычка считается при раскладке, а не при сборке: сдвиг карты
 * двигает ярлычки, не пересобирая их. Проверяется то, ради чего это
 * опасно: после сдвига ярлычок стоит там, куда карту сдвинули, и
 * нажимается там же, а не на прежнем месте.
 */
class MapMarksTest {

    @Test
    fun `после сдвига карты ярлычок нажимается на новом месте`() {
        val state = MapState(LATITUDE, LONGITUDE, ZOOM)
        val mark = MapMark("place", LATITUDE, LONGITUDE, 3, Color.Red, chosen = false)
        var picked: String? = null
        RenderProbe(WIDTH, HEIGHT) {
            Box(Modifier.fillMaxSize()) { MapMarks(state, IntSize(WIDTH, HEIGHT), listOf(mark)) { picked = it.id } }
        }.use { probe ->
            probe.frame()
            state.pan(SHIFT, SHIFT)
            probe.frame()
            probe.click(Offset(WIDTH / 2f, HEIGHT / 2f))
            assertEquals(null, picked, "ярлычок остался на прежнем месте")
            probe.click(Offset(WIDTH / 2f + SHIFT, HEIGHT / 2f + SHIFT))
        }
        assertEquals("place", picked)
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 600
        const val ZOOM = 14
        const val LATITUDE = 43.238
        const val LONGITUDE = 76.945
        const val SHIFT = 120f
    }
}
