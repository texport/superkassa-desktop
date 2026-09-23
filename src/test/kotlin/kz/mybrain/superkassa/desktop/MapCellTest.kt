package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.analytics.groupCell
import kz.mybrain.superkassa.desktop.ui.map.MapPixel
import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import kz.mybrain.superkassa.desktop.ui.theme.AnalyticsLayout
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Кружок места не выходит за свою клетку.
 *
 * Кружки двух соседних клеток, прижатых к общей границе, прежде
 * ложились один на другой. Внутри своей клетки каждый — и между ними
 * остаётся воздух, на экране любой плотности.
 */
class MapCellTest {

    private val cell = groupCell(1f)
    private val radius = Sizes.mapMarkCrowd.value / 2.0
    private val corner = MapPixel(CORNER, CORNER)

    @Test
    fun `кружки по обе стороны границы клеток не наезжают`() {
        val left = MapProjection.keptInCell(MapPixel(border() - 1, MIDDLE), corner, cell, radius)
        val right = MapProjection.keptInCell(MapPixel(border() + 1, MIDDLE), corner, cell, radius)
        assertTrue(right.x - left.x >= radius * 2, "между кружками ${right.x - left.x} точек")
    }

    @Test
    fun `кружок в середине клетки стоит на месте`() {
        val middle = MapPixel(border() + cell / 2, border() + cell / 2)
        assertEquals(middle, MapProjection.keptInCell(middle, corner, cell, radius))
    }

    @Test
    fun `клетка растёт с плотностью экрана`() {
        assertEquals(AnalyticsLayout.mapCell.value * 2.0, groupCell(2f))
        assertTrue(groupCell(1f) > Sizes.mapMarkCrowd.value, "клетка уже самого крупного кружка")
    }

    /** Граница клеток в окне: окно начинается не с границы, а внутри клетки. */
    private fun border(): Double = cell * 3 - CORNER

    private companion object {
        const val CORNER = 1000.0
        const val MIDDLE = 100.0
    }
}
