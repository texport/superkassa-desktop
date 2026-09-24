package kz.mybrain.superkassa.presentation.analytics.sales

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.model.regionsOf
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.common.analyticsTroubleState
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.journal.journalTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.refusal
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

    /** Сеть выросла: стрелки вверх и зелёный цвет у всех итогов. */
    @Test
    fun `итоги с ростом`() = body("an-sales-growth", SalesLook.growing())

    /** Сеть просела: те же итоги стрелками вниз и цветом отказа. */
    @Test
    fun `итоги с падением`() = body("an-sales-fall", SalesLook.falling())

    /** Регионы: семь строк со своими долями сети. */
    @Test
    fun `свод по регионам`() {
        val view = SalesLook.regioned()
        val regions = regionsOf(view.places, view.registers, view.retailPlaces, AnalyticsLook.texts.sales.noAddress)
        val sales = AnalyticsLook.texts.sales
        RenderProbe(WIDE, HIGH) {
            SectionCard(sales.regions, info = sales.regionsHint) { SalesRegions(regions, sales) }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-sales-regions", probe.frame())
        }
    }

    /** Та же сеть целой сводкой: свод регионов стоит в ней своей карточкой. */
    @Test
    fun `сводка с регионами`() = body("an-sales-with-regions", SalesLook.regioned())

    /** Один день: столбик один, и ряд не должен выглядеть сломанным. */
    @Test
    fun `один день`() = body("an-sales-one-day", SalesLook.view(days = 1, rows = 1))

    /** Месяц: тридцать столбиков в тот же ряд. */
    @Test
    fun `месяц`() = body("an-sales-month", SalesLook.view(days = SalesLook.MONTH))

    /** Выручка нулевая: нули по всем плиткам, и об этом сказано словами. */
    @Test
    fun `выручка нулевая`() = body("an-sales-zero", SalesLook.nothingSold())

    /**
     * Сеть показа: полтора месяца, 81 чек и нулевой НДС на три тысячи касс.
     *
     * Снимается вся сводка сверху донизу, а не первый экран: провал
     * в графике, доли расчётов, таблицы и свод по регионам стоят ниже
     * сгиба, и именно там экран выглядит сломанным, когда данных почти нет.
     */
    @Test
    fun `сеть показа`() {
        val view = SalesShowLook.show()
        RenderProbe(WIDE, HIGH) {
            AnalyticsSalesBody(view, AnalyticsLook.texts, enums, journal, Look.cabinet, Modifier.fillMaxSize())
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("audit-analytics-sales-show", probe.frame())
            repeat(DOWN) { at ->
                repeat(TURNS) { probe.wheel(MIDDLE, SCROLL) }
                Look.shot("audit-analytics-sales-show-${at + 1}", probe.frame())
            }
        }
    }

    /** Единственный вид расчёта: доля обязана быть целой. */
    @Test
    fun `единственный вид расчётов`() = body("an-sales-one-payment", SalesLook.onlyCash())

    /** Сотня строк в таблице: столбцы не должны разъехаться. */
    @Test
    fun `сотня строк в таблице`() {
        val rows = (1..SalesLook.HUNDRED).map(SalesLook::unit)
        RenderProbe(WIDE, HIGH) {
            val texts = AnalyticsLook.texts
            SalesTable(rows, SalesRows.Registers, texts, journal, texts.sales.allRegistersShown)
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-sales-hundred-rows", probe.frame())
        }
    }

    /** Таблица по точкам за пустой срок: пустота обязана быть объяснена. */
    @Test
    fun `таблица без строк`() {
        RenderProbe(WIDE, HIGH) {
            val texts = AnalyticsLook.texts
            SalesTable(emptyList(), SalesRows.Places, texts, journal, texts.sales.allPlacesShown)
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-sales-no-rows", probe.frame())
        }
    }

    /** Пустой срок: на месте сводки объяснение, а не пустота. */
    @Test
    fun `пустой срок`() {
        val sales = AnalyticsLook.texts.sales
        val state = ScreenState.Empty(AppIcons.noDocuments, sales.empty, sales.emptyHint)
        shotOfState("an-sales-empty-period", state)
    }

    /** Кабинет не ответил: помеха со словами о связи и кнопкой повтора. */
    @Test
    fun `кабинет не ответил`() =
        shotOfState("an-sales-unreachable", analyticsTroubleState(AnalyticsTrouble.Unreachable, AnalyticsLook.texts) {})

    /** Кабинет отказал по существу: отказ показывается его же словами. */
    @Test
    fun `кабинет отказал`() {
        val refusal = AnalyticsTrouble.Refused("Доступ к сводке выдан не этой компании")
        shotOfState("an-sales-refused", analyticsTroubleState(refusal, AnalyticsLook.texts) {})
    }

    /** Раздел ещё не выложен: владельцу здесь чинить нечего. */
    @Test
    fun `раздел не выложен`() {
        val state = analyticsTroubleState(AnalyticsTrouble.NotDeployed, AnalyticsLook.texts) {}
        shotOfState("an-sales-not-deployed", state)
    }

    private fun body(name: String, view: SalesView) {
        RenderProbe(WIDE, HIGH) {
            AnalyticsSalesBody(view, AnalyticsLook.texts, enums, journal, Look.cabinet, Modifier.fillMaxSize())
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
        /** Куда наводится колесо: середина сводки. */
        val MIDDLE = Offset(WIDE / 2f, HIGH / 2f)

        /** Сколько раз сводка прокручивается вниз и на сколько за раз. */
        const val DOWN = 3
        const val SCROLL = 8f

        /** Оборотов колеса на один экран сводки. */
        const val TURNS = 8

        const val WIDE = 1180
        const val HIGH = 820
    }
}
