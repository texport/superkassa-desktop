package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.analytics.RecordRegionRow
import kz.mybrain.superkassa.desktop.ui.analytics.RecordRegionsHead
import kz.mybrain.superkassa.desktop.ui.analytics.recordRegions
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Область названа и в узком окне.
 *
 * Чисел в таблице учёта восемь — больше, чем в любой другой таблице
 * раздела, — и восемь столбцов заданной ширины в окне тысячу шириной
 * не оставляли названию области ничего: строки начинались с числа
 * точек, а какой области они принадлежат, экран не говорил вовсе.
 *
 * Меряется чернилами левого края строк: названия областей в обоих окнах
 * одни и те же и набраны от одного края.
 */
class AnalyticsRecordNarrowTest {

    private val regions = recordRegions(RecordFleet.show(count = FLEET), "Без адреса")

    @Test
    fun `название области нарисовано и в широком окне, и в узком`() {
        val wide = namesInk(WIDE)
        val narrow = namesInk(NARROW)
        assertTrue(wide > 0, "названий областей не нарисовалось вовсе")
        assertEquals(wide, narrow, "в окне $NARROW точек от названия области осталось $narrow против $wide")
    }

    /** Чернила левого края таблицы ниже подписей столбцов. */
    private fun namesInk(width: Int): Int {
        val png = RenderProbe(width, HEIGHT) {
            Column(Modifier.fillMaxWidth()) {
                RecordRegionsHead(Look.texts)
                regions.forEach { RecordRegionRow(it) }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        val image = ImageIO.read(ByteArrayInputStream(png))
        val paper = image.getRGB(0, 0)
        val strip = image.getRGB(0, BELOW_HEAD, NAME_WIDTH, image.height - BELOW_HEAD, null, 0, NAME_WIDTH)
        return strip.count { it != paper }
    }

    private companion object {
        const val FLEET = 40
        const val WIDE = 1400
        const val NARROW = 1000
        const val HEIGHT = 300
        const val SETTLE = 12

        /** Ниже этой строки идут сами области, а не подписи столбцов. */
        const val BELOW_HEAD = 60

        /** Полоса, в которую название области обязано умещаться в обоих окнах. */
        const val NAME_WIDTH = 80
    }
}
