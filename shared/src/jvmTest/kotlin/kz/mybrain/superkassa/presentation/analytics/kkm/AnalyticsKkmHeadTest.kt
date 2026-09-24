package kz.mybrain.superkassa.presentation.analytics.kkm

import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.analytics.CabinetReplies
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.analyzing
import kz.mybrain.superkassa.presentation.analytics.common.shiftPlate
import kz.mybrain.superkassa.presentation.analytics.map.MapWords
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Шапка окна аналитики одной кассы: открытая смена видна и при длинном адресе.
 *
 * Строка под названием кассы обрывается многоточием, а смена стояла в ней
 * последней: у кассы с адресом в полторы строки окно 960×640 смену
 * не показывало вовсе.
 */
class AnalyticsKkmHeadTest {

    private val kkm = AnalyticsLook.kkm(1, address = LONG_ADDRESS, shiftOpen = true)
    private val shift = requireNotNull(shiftPlate(kkm.shiftStatus, kkm.shiftNumber, Look.cabinet))

    @Test
    fun `смена стоит первой и не уходит за многоточие`() {
        val about = kkmAbout(kkm, Look.cabinet)
        assertTrue(about.startsWith(shift), "смена не первой: «$about»")
    }

    @Test
    fun `окно кассы в наименьшем окне говорит о смене`() {
        val app = CoreScene.app(FakeCore()).analyzing(CabinetReplies.always(NOTHING).analytics)
        RenderProbe(NARROW, LOW) {
            AnalyticsKkmDialog(app, kkm, OWNER, MapWords(AnalyticsLook.texts, Look.cabinet)) {}
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("qa-analytics-kkm-dialog-960", probe.frame())
            val head = probe.nodes().first { it.text.contains(LONG_ADDRESS) }
            assertTrue(head.text.startsWith(shift), "шапка: «${head.text}»")
        }
    }

    private companion object {
        const val LONG_ADDRESS = "Солтүстік Қазақстан облысы, Мағжан Жұмабаев ауданы, Тәуелсіздік даңғылы, 1"
        const val OWNER = "owner-1"
        const val NOTHING = "{}"
        const val NARROW = 960
        const val LOW = 640
        const val SETTLE = 24
    }
}
