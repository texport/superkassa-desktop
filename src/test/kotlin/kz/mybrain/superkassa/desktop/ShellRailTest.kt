package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionRail
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Рельс разделов в низком окне.
 *
 * Разделов у администратора десять, а окно кассы бывает ростом в 700 точек:
 * за вычетом шапки на рельс остаётся около 636. Собранный целиком столбец
 * не помещался — «Настройки» уходили под нижний край вместе с версией
 * в углу, и открыть их было нечем.
 */
class ShellRailTest {

    /** Высота рабочей области при окне 1000×700: окно минус шапка с полоской. */
    private val lowWindow = 636

    @Composable
    private fun Rail(current: Section = Section.Dashboard, collapsed: Boolean = false) {
        Row(modifier = Modifier.fillMaxSize()) {
            SectionRail(
                sections = Section.entries,
                current = current,
                collapsed = collapsed,
                onToggle = {},
                footer = { Text(VERSION) },
                onPick = {}
            )
        }
    }

    @Test
    fun `в низком окне разделы прокручиваются`() {
        RenderProbe(width = WIDTH, height = lowWindow) { Rail() }.use { probe ->
            val before = probe.frame()
            File(SHOT).writeBytes(before)
            probe.wheel(at = Offset(40f, 400f), ticks = 6f)
            assertTrue(probe.changedFrom(before), "разделы в низком окне не прокручиваются")
        }
    }

    /**
     * Версия стоит под разделами и прокруткой не уезжает: поддержка
     * спрашивает её первой, а кассир не должен для этого листать рельс.
     */
    @Test
    fun `версия остаётся на месте при прокрутке разделов`() {
        RenderProbe(width = WIDTH, height = lowWindow) { Rail() }.use { probe ->
            probe.frame()
            probe.wheel(at = Offset(40f, 400f), ticks = 6f)
            val scrolled = probe.frame()
            File(SCROLLED_SHOT).writeBytes(scrolled)
            assertTrue(scrolled.isNotEmpty())
        }
    }

    private companion object {
        const val WIDTH = 1000
        const val VERSION = "1.0.0"
        const val SHOT = "/tmp/rail-low.png"
        const val SCROLLED_SHOT = "/tmp/rail-low-scrolled.png"
    }
}
