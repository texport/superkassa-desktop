package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsSalesBody
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.SalesRows
import kz.mybrain.superkassa.desktop.ui.analytics.SalesTable
import kz.mybrain.superkassa.desktop.ui.analytics.SalesView
import kz.mybrain.superkassa.desktop.ui.analytics.analyticsTroubleState
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kotlin.test.Test

/**
 * Снимки торговой сводки: обычный срок и все отказные.
 *
 * Здесь смотрят на числа: разделены ли разряды у сотен миллионов тенге,
 * не съезжают ли столбцы таблицы на сотне строк и говорит ли экран
 * словами, что за срок ничего не продано.
 */
class AnalyticsSalesShots {

    private val enums = stringsOf(Language.Ru).enums
    private val journal = journalTexts(Language.Ru).history

    /** Неделя торговли: главные числа, столбики, доли и таблицы. */
    @Test
    fun `неделя торговли`() = body("an-sales-week", SalesLook.view())

    /** Один день: столбик один, и ряд не должен выглядеть сломанным. */
    @Test
    fun `один день`() = body("an-sales-one-day", SalesLook.view(days = 1, rows = 1))

    /** Месяц: тридцать столбиков в тот же ряд. */
    @Test
    fun `месяц`() = body("an-sales-month", SalesLook.view(days = SalesLook.MONTH))

    /** Выручка нулевая: нули по всем плиткам, и об этом сказано словами. */
    @Test
    fun `выручка нулевая`() = body("an-sales-zero", SalesLook.nothingSold())

    /** Единственный вид расчёта: доля обязана быть целой. */
    @Test
    fun `единственный вид расчётов`() = body("an-sales-one-payment", SalesLook.onlyCash())

    /** Сотня строк в таблице: столбцы не должны разъехаться. */
    @Test
    fun `сотня строк в таблице`() {
        val rows = (1..SalesLook.HUNDRED).map(SalesLook::unit)
        RenderProbe(WIDE, HIGH) {
            SalesTable(rows, SalesRows.Registers, Look.texts, journal, Look.texts.sales.allRegistersShown)
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-sales-hundred-rows", probe.frame())
        }
    }

    /** Таблица по точкам за пустой срок: пустота обязана быть объяснена. */
    @Test
    fun `таблица без строк`() {
        RenderProbe(WIDE, HIGH) {
            SalesTable(emptyList(), SalesRows.Places, Look.texts, journal, Look.texts.sales.allPlacesShown)
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-sales-no-rows", probe.frame())
        }
    }

    /** Пустой срок: на месте сводки объяснение, а не пустота. */
    @Test
    fun `пустой срок`() {
        val state = ScreenState.Empty(AppIcons.noDocuments, Look.texts.sales.empty, Look.texts.sales.emptyHint)
        shotOfState("an-sales-empty-period", state)
    }

    /** Кабинет не ответил: помеха со словами о связи и кнопкой повтора. */
    @Test
    fun `кабинет не ответил`() =
        shotOfState("an-sales-unreachable", analyticsTroubleState(AnalyticsTrouble.Unreachable, Look.texts) {})

    /** Кабинет отказал по существу: отказ показывается его же словами. */
    @Test
    fun `кабинет отказал`() {
        val refusal = AnalyticsTrouble.Refused("Доступ к сводке выдан не этой компании")
        shotOfState("an-sales-refused", analyticsTroubleState(refusal, Look.texts) {})
    }

    /** Раздел ещё не выложен: владельцу здесь чинить нечего. */
    @Test
    fun `раздел не выложен`() =
        shotOfState("an-sales-not-deployed", analyticsTroubleState(AnalyticsTrouble.NotDeployed, Look.texts) {})

    private fun body(name: String, view: SalesView) {
        RenderProbe(WIDE, HIGH) {
            AnalyticsSalesBody(view, Look.texts, enums, journal, Look.cabinet, Modifier.fillMaxSize())
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot(name, probe.frame())
        }
    }

    private fun shotOfState(name: String, state: ScreenState) {
        RenderProbe(WIDE, HIGH) { ScreenSlot(state, Modifier.fillMaxSize()) {} }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    private companion object {
        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
