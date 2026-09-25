package kz.mybrain.superkassa.presentation.analytics

import kz.mybrain.superkassa.data.analytics.AnalyticsFixtures
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.designsystem.theme.size.AnalyticsLayout
import kz.mybrain.superkassa.strings.api.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Аналитика на мониторах и планшетах.
 *
 * Окна — от наименьшего окна кассы до широкого монитора и планшеты
 * стоймя и лёжа; ступени шрифта обычная и крупнейшая; русский и казахский;
 * светлое и тёмное оформление. Данные — предельные ([AnalyticsFixtures]),
 * а если рядом лежат снятые с кабинета — ещё и живые.
 *
 * Кадры остаются в `/tmp/adaptive-analytics-<набор>-<окно>-<ступень>-<язык>-<вкладка>.png`.
 */
class AnalyticsAdaptiveTest {

    private fun walk(
        set: String,
        reply: (String) -> String?,
        size: Pair<Int, Int>,
        scale: TextScale,
        language: Language,
        dark: Boolean = false
    ) {
        val (width, height) = size
        val tag = "$set-${width}x$height-${scale.code}-${language.name.lowercase()}" + if (dark) "-dark" else ""
        val appearance = if (dark) Appearance.Dark else Appearance.Light
        AnalyticsWindow(width, height, language, Look(textScale = scale), appearance, reply).use { window ->
            val walk = AnalyticsWalk(window, tag)
            val measure = walk.mapPart()
            val reachable = measure.kkmSalesVisible || walk.kkmSalesReachable()
            println("$tag: $measure reachable=$reachable")
            check(tag, measure, reachable)
            assertTrue(walk.kkmDialogFits(), "$tag: окно аналитики кассы не помещается в окно")
            walk.otherTabs()
        }
    }

    /**
     * Карта остаётся картой: не ниже [AnalyticsLayout.mapLeast] и до выбора
     * кассы, и после; «Аналитика кассы» достаётся колесом; кнопки карты
     * не лежат на легенде; «Обновить» не уходит за край; кружки мест
     * не наезжают друг на друга.
     */
    private fun check(tag: String, measure: AnalyticsWalk.Measure, reachable: Boolean) {
        val least = AnalyticsLayout.mapLeast.value - 1
        assertTrue(measure.mapHeight >= least, "$tag: карта ${measure.mapHeight} точек")
        assertTrue(measure.mapHeightChosen >= least, "$tag: карта при выбранной кассе ${measure.mapHeightChosen} точек")
        assertTrue(reachable, "$tag: «Аналитика кассы» не достать ни видом, ни прокруткой")
        assertFalse(measure.controlsOverLegend, "$tag: кнопки карты лежат на легенде")
        assertTrue(measure.refreshInside, "$tag: «Обновить» за краем окна")
        assertEquals(0, measure.marksOverlapping, "$tag: кружки мест наезжают друг на друга")
    }

    @Test
    fun `предельные данные на всех окнах`() {
        val reply = AnalyticsFixtures.extreme()
        SIZES.forEach { size -> walk("extreme", reply, size, TextScale.Normal, Language.Ru) }
    }

    @Test
    fun `крупная ступень и казахский язык`() {
        val reply = AnalyticsFixtures.extreme()
        listOf(960 to 640, 1180 to 820, 800 to 1280).forEach { size ->
            walk("extreme", reply, size, TextScale.Larger, Language.Kk)
        }
        walk("extreme", reply, 1180 to 820, TextScale.Normal, Language.Ru, dark = true)
    }

    @Test
    fun `живые данные кабинета`() {
        val reply = AnalyticsFixtures.live() ?: return
        listOf(960 to 640, 1180 to 820, 800 to 1280, 2560 to 1080).forEach { size ->
            walk("live", reply, size, TextScale.Normal, Language.Ru)
        }
        walk("live", reply, 960 to 640, TextScale.Larger, Language.Kk)
    }

    private companion object {
        val SIZES = listOf(960 to 640, 1180 to 820, 1920 to 1080, 2560 to 1080, 800 to 1280, 1280 to 800)
    }
}
