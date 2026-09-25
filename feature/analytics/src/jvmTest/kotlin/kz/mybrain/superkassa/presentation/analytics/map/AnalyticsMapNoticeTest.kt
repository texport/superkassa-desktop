package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Карта касс без плиток говорит о себе, а не об окне выбора места.
 *
 * Надпись о неприехавших плитках была общей с окном выбора места
 * и обещала, что «место всё равно ставится нажатием». На карте касс
 * нажатие ничего не ставит — оно снимает выбор, — и владелец искал,
 * куда делась поставленная им точка.
 */
class AnalyticsMapNoticeTest {

    @Test
    fun `без плиток карта касс не обещает поставить место нажатием`() {
        val view = AnalyticsLook.view((1..3).map { AnalyticsLook.kkm(it, address = HERE) })
        val start = AnalyticsLook.model()
        val laid = laidOut(view, mapOf(HERE to (AnalyticsLook.LATITUDE to AnalyticsLook.LONGITUDE)))
        AnalyticsLook.centre(start, laid.placed)
        RenderProbe(WIDE, HIGH) { MapLook(start, laid, groupsOf(laid, start.map.zoom), view) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("qa-analytics-map-no-tiles", probe.frame())
            val texts = probe.nodes().map { it.text }
            assertTrue(texts.any { AnalyticsLook.texts.mapNoTiles in it }, "о плитках не сказано ничего")
            assertFalse(texts.any { Look.cabinet.map.noTiles in it }, "карта касс говорит словами окна выбора места")
        }
    }

    private companion object {
        const val HERE = "г. Алматы, пр. Абая, 10"
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 24
    }
}
