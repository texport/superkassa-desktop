package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.SalesDay
import kz.mybrain.superkassa.desktop.ui.analytics.SalesChart
import kz.mybrain.superkassa.desktop.ui.analytics.dayBars
import kz.mybrain.superkassa.desktop.ui.history.JournalRange
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Дата под графиком читается на сроке любой длины.
 *
 * Подпись оси стояла в делении своего столбика, а деление у длинного
 * срока уже самой даты: за полтора месяца — те самые, за которые
 * считается сводка показа, — от «01.09» оставалось «01.0» с обрезанной
 * посередине цифрой.
 *
 * Меряется чернилами первой подписи: она у обоих сроков одна и та же
 * и стоит на одном и том же месте, а её ширина зависела от длины срока.
 */
class AnalyticsChartAxisTest {

    private val texts = analyticsTexts(Language.Ru).sales
    private val first: LocalDate = LocalDate.parse("2026-09-01")

    @Test
    fun `первая дата оси нарисована целиком и за неделю, и за полтора месяца`() {
        val week = axisInk(WEEK)
        val long = axisInk(LONG)
        assertTrue(week > 0, "подписи оси не нарисовалось вовсе")
        assertEquals(week, long, "дата за срок в $LONG суток обрезана: $long точек против $week")
    }

    /** Сколько точек чернил у первой подписи оси. */
    private fun axisInk(days: Int): Int {
        val range = JournalRange(first, first.plusDays(days - 1L))
        val rows = listOf(SalesDay(first.toString(), 1, BigDecimal("1000.00")))
        val png = RenderProbe(WIDTH, HEIGHT) { SalesChart(dayBars(rows, range, texts), texts) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                probe.frame()
            }
        return ink(png)
    }

    /**
     * Точки первой подписи оси.
     *
     * Подпись — самое нижнее, что есть в графике, и полоса берётся от неё
     * вверх: высота самого полотна задана оформлением, и считать её здесь
     * значило бы повторить её числом.
     */
    private fun ink(png: ByteArray): Int {
        val image = ImageIO.read(ByteArrayInputStream(png))
        val paper = image.getRGB(0, 0)
        val all = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        val bottom = (image.height - 1 downTo 0).first { row ->
            (0 until image.width).any { all[row * image.width + it] != paper }
        }
        val from = (bottom - AXIS_STRIP + 1).coerceAtLeast(0)
        val strip = image.getRGB(0, from, LABEL_WIDTH, bottom - from + 1, null, 0, LABEL_WIDTH)
        return strip.count { it != paper }
    }

    private companion object {
        const val WIDTH = 1180
        const val HEIGHT = 320
        const val SETTLE = 12
        const val WEEK = 7

        /** Полтора месяца: столько считает сводка показа. */
        const val LONG = 53

        /** Полоса кадра, в которой стоит ось. */
        const val AXIS_STRIP = 20

        /** Ширина, в которую подпись обязана уместиться целиком. */
        const val LABEL_WIDTH = 60
    }
}
