package kz.mybrain.superkassa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.data.cabinet.SalesDay
import kz.mybrain.superkassa.data.cabinet.SalesDelivery
import kz.mybrain.superkassa.data.cabinet.SalesDeliveryCounts
import kz.mybrain.superkassa.data.cabinet.SalesHour
import kz.mybrain.superkassa.data.cabinet.SalesPayments
import kz.mybrain.superkassa.data.cabinet.SalesSummary
import kz.mybrain.superkassa.data.cabinet.SalesUnit
import kz.mybrain.superkassa.presentation.analytics.AnalyticsSalesBody
import kz.mybrain.superkassa.presentation.analytics.SalesChart
import kz.mybrain.superkassa.presentation.analytics.SalesPurchaseTiles
import kz.mybrain.superkassa.presentation.analytics.SalesShares
import kz.mybrain.superkassa.presentation.analytics.SalesView
import kz.mybrain.superkassa.presentation.analytics.dayBars
import kz.mybrain.superkassa.presentation.analytics.hourBars
import kz.mybrain.superkassa.presentation.analytics.salesShares
import kz.mybrain.superkassa.presentation.components.ScreenSlot
import kz.mybrain.superkassa.presentation.components.ScreenState
import kz.mybrain.superkassa.presentation.history.JournalRange
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.journalTexts
import kz.mybrain.superkassa.presentation.strings.stringsOf
import kz.mybrain.superkassa.presentation.theme.AppIcons
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Торговая сводка рисуется на сцене без окна.
 *
 * Кабинет здесь не спрашивается: до выкладки его ручек нет, а проверять
 * нужно другое — что полотно со столбиками собирается и не падает
 * ни на пустом ряде, ни на месячном, и что пустой срок оборачивается
 * объяснением, а не пустым экраном.
 */
class AnalyticsSalesRenderTest {

    private val texts = analyticsTexts(Language.Ru)
    private val enums = stringsOf(Language.Ru).enums
    private val journal = journalTexts(Language.Ru).history
    private val cabinet = cabinetTexts(Language.Ru)

    private fun money(value: String) = BigDecimal(value)

    private fun day(at: Int) = SalesDay(
        date = "2026-09-%02d".format(at),
        receiptCount = at,
        revenue = money("${at * 1000}.00"),
        refunds = money("0.00")
    )

    private fun unit(at: Int) = SalesUnit(
        id = "c$at",
        registrationNumber = "%012d".format(at),
        name = "Касса $at",
        retailPlaceName = "Магазин $at",
        receiptCount = at,
        revenue = money("${at * 1000}.00"),
        difference = money("${at * 900}.00"),
        lastContactAt = "2026-09-19T08:14:00Z"
    )

    private fun view(days: Int = DAYS, rows: Int = ROWS) = SalesView(
        range = JournalRange(FIRST_DAY, FIRST_DAY.plusDays(days - 1L)),
        summary = SalesSummary(
            receiptCount = 128,
            revenue = money("1284560.00"),
            refunds = money("12400.50"),
            tax = money("137631.43"),
            payments = SalesPayments(cash = money("400000.00"), card = money("884560.00")),
            offlineCount = 3,
            queuedCount = 2,
            unknownCount = 5,
            cashRegisterCount = rows,
            openShiftCount = 4
        ),
        days = (1..days).map(::day),
        hours = (0 until HOURS).map { SalesHour(it, it, money("${it * 100}.00")) },
        registers = (1..rows).map(::unit),
        places = (1..rows).map(::unit),
        delivery = SalesDelivery(
            receipts = SalesDeliveryCounts(
                total = 312, delivered = 300, queued = 5, unknown = 5, rejected = 2
            ),
            offlineCount = 7
        )
    )

    @Test
    fun `сводка целиком собирается и рисуется`() {
        RenderProbe { AnalyticsSalesBody(view(), texts, enums, journal, cabinet, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `покупка у населения рисуется отдельно, а без неё сводка прежняя`() {
        val bought = view().let { it.copy(summary = it.summary.copy(purchaseCount = 4, purchases = money("7500.00"))) }
        RenderProbe { AnalyticsSalesBody(bought, texts, enums, journal, cabinet, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
        RenderProbe { SalesPurchaseTiles(bought.summary, texts.sales, cabinet) }
            .use { assertTrue(it.frame().isNotEmpty()) }
        assertTrue(!view().summary.purchased, "без полей кабинета карточки покупки нет вовсе")
    }

    @Test
    fun `столбики рисуются и за месяц, и за сутки, и без единой продажи`() {
        val sales = texts.sales
        RenderProbe { SalesChart(dayBars(view(days = MONTH).days, view(days = MONTH).range, sales), sales, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
        RenderProbe { SalesChart(dayBars(view(days = 1).days, view(days = 1).range, sales), sales, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
        RenderProbe { SalesChart(hourBars(emptyList(), sales), sales, Modifier.fillMaxSize()) }
            .use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `доли видов расчётов рисуются, а пустые оборачиваются строкой`() {
        val sales = texts.sales
        RenderProbe {
            SalesShares(salesShares(view().summary.payments, enums, sales.paymentOther), sales)
        }.use { assertTrue(it.frame().isNotEmpty()) }
        RenderProbe {
            SalesShares(salesShares(SalesPayments(), enums, sales.paymentOther), sales)
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    @Test
    fun `за пустой срок на месте сводки стоит объяснение, а не пустота`() {
        RenderProbe {
            val state = ScreenState.Empty(AppIcons.noDocuments, texts.sales.empty, texts.sales.emptyHint)
            ScreenSlot(state, Modifier.fillMaxSize()) {}
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    private companion object {
        val FIRST_DAY: LocalDate = LocalDate.parse("2026-09-01")
        const val DAYS = 7
        const val MONTH = 30
        const val HOURS = 24
        const val ROWS = 12
    }
}
