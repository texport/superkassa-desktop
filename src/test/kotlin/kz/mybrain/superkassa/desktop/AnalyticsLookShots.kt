package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsRecordBody
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSalesBody
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kotlin.test.Test

/**
 * Раздел аналитики на трёх языках, в тёмном оформлении и в узком окне.
 *
 * Числа показа везде одни и те же, а разойтись может разметка: казахская
 * подпись вдвое длиннее русской, тёмная тема меняет роли цвета, а окно
 * в тысячу точек отнимает у плиток треть ширины. Смотрит на это человек —
 * снимок для того и делается.
 */
class AnalyticsLookShots {

    /** Торговая сводка показа на трёх языках. */
    @Test
    fun `торговля на трёх языках`() = Language.entries.forEach { language ->
        shot("audit-analytics-sales-${language.name.lowercase()}", language, WIDE, HIGH) { sales(language) }
    }

    /** Учёт касс на трёх языках: заголовки столбцов там длиннее всего. */
    @Test
    fun `учёт касс на трёх языках`() = Language.entries.forEach { language ->
        shot("audit-analytics-record-${language.name.lowercase()}", language, WIDE, HIGH) { record(language) }
    }

    /** Узкое окно: тысяча на семьсот — самое малое, в котором работают. */
    @Test
    fun `узкое окно`() {
        shot("audit-analytics-sales-narrow", Language.Ru, NARROW, LOW) { sales(Language.Ru) }
        shot("audit-analytics-record-narrow", Language.Ru, NARROW, LOW) { record(Language.Ru) }
    }

    /** Тёмное оформление: цвет там несёт тот же смысл, что и в светлом. */
    @Test
    fun `тёмное оформление`() {
        shot("audit-analytics-sales-dark", Language.Ru, WIDE, HIGH, Appearance.Dark) { sales(Language.Ru) }
        shot("audit-analytics-record-dark", Language.Ru, WIDE, HIGH, Appearance.Dark) { record(Language.Ru) }
    }

    @Composable
    private fun sales(language: Language) = AnalyticsSalesBody(
        view = SalesLook.show(),
        texts = analyticsTexts(language),
        enums = stringsOf(language).enums,
        journal = journalTexts(language).history,
        cabinet = cabinetTexts(language),
        modifier = Modifier.fillMaxSize()
    )

    @Composable
    private fun record(language: Language) =
        AnalyticsRecordBody(RecordFleet.show(), analyticsTexts(language), Modifier.fillMaxSize())

    private fun shot(
        name: String,
        language: Language,
        width: Int,
        height: Int,
        appearance: Appearance = Appearance.Light,
        content: @Composable () -> Unit
    ) = RenderProbe(width, height, appearance, language = language, content = content).use { probe ->
        repeat(SETTLE) { probe.frame() }
        Look.shot(name, probe.frame())
    }

    private companion object {
        const val SETTLE = 24
        const val WIDE = 1400
        const val HIGH = 900

        /** Самое малое окно, в котором работают: тысяча на семьсот. */
        const val NARROW = 1000
        const val LOW = 700
    }
}
