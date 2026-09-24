package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Кнопки увеличения у предела гаснут, а не молчат.
 *
 * На самом крупном увеличении «Приблизить» нажималась и ничего не делала,
 * и так же «Отдалить» на самом мелком: кнопка, которая не отвечает,
 * читается как поломка карты.
 */
class MapControlsTest {

    @Test
    fun `у предела увеличения кнопка в его сторону погашена`() {
        assertEquals(false to true, buttons(MapState(zoom = MAX_ZOOM)), "на самом крупном увеличении")
        assertEquals(true to false, buttons(MapState(zoom = MIN_ZOOM)), "на самом мелком увеличении")
        assertEquals(true to true, buttons(MapState()), "посередине")
    }

    /** Доступность «Приблизить» и «Отдалить». */
    private fun buttons(state: MapState): Pair<Boolean, Boolean> {
        val locating = AnalyticsLook.tools().locating
        return RenderProbe(WIDTH, HEIGHT) { MapControls(state, Look.cabinet, locating) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.nodes { nodes ->
                enabled(nodes, Look.cabinet.map.zoomIn) to enabled(nodes, Look.cabinet.map.zoomOut)
            }
        }
    }

    /** Доступна ли кнопка, значок которой подписан [label]. */
    private fun enabled(nodes: List<SemanticsNode>, label: String): Boolean {
        val button = nodes.first { node ->
            node.config.getOrNull(SemanticsProperties.Role) != null &&
                node.children.any { label in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
        }
        return !button.config.contains(SemanticsProperties.Disabled)
    }

    private companion object {
        const val WIDTH = 400
        const val HEIGHT = 400
        const val SETTLE = 6
    }
}
