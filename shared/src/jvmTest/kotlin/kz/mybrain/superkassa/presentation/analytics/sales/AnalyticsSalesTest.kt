package kz.mybrain.superkassa.presentation.analytics.sales

import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kz.mybrain.superkassa.domain.analytics.model.SALES_MOST_DAYS
import kz.mybrain.superkassa.domain.analytics.model.SalesDay
import kz.mybrain.superkassa.domain.analytics.model.SalesDelivery
import kz.mybrain.superkassa.domain.analytics.model.SalesPayments
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.model.SalesUnit
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.domain.analytics.model.tiynOf
import kz.mybrain.superkassa.presentation.analytics.sales.chart.salesShares
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Счёт и порядок торговой сводки.
 *
 * Всё, что здесь проверяется, считается без кабинета и без окна: доли
 * видов расчётов, порядок таблиц, границы срока и высота столбиков.
 * Ошибка в любом из них молча показывает владельцу неверные числа.
 */
class AnalyticsSalesTest {

    private val texts = textsOf(Language.Ru).analytics.sales
    private val enums = textsOf(Language.Ru).common.enums
    private val today = LocalDate.parse("2026-09-20")

    private fun sum(value: String): Long = tiynOf(value)

    // --- Виды расчётов ---

    @Test
    fun `ноль не становится долей, а остальные складываются в целое`() {
        val shares = salesShares(
            SalesPayments(cash = sum("250.00"), card = sum("750.00"), mobile = 0L),
            enums,
            texts.paymentOther
        )
        assertEquals(2, shares.size)
        assertEquals(enums.paymentCard, shares.first().title)
        assertEquals(75, shares.first().percent)
        assertEquals(25, shares.last().percent)
    }

    @Test
    fun `без единого расчёта долей нет вовсе`() {
        assertTrue(salesShares(SalesPayments(), enums, texts.paymentOther).isEmpty())
    }

    @Test
    fun `прочее названо своим словом и стоит наравне с остальными`() {
        val shares = salesShares(SalesPayments(other = sum("100.00")), enums, texts.paymentOther)
        assertEquals(texts.paymentOther, shares.single().title)
        assertEquals(100, shares.single().percent)
    }

    // --- Порядок таблиц ---

    private fun unit(name: String, receipts: Int, revenue: String) =
        SalesUnit(id = name, name = name, receiptCount = receipts, revenue = sum(revenue))

    private val units = listOf(
        unit("Первая", 10, "100.00"),
        unit("Вторая", 30, "50.00"),
        unit("Третья", 20, "900.00")
    )

    @Test
    fun `таблица выстраивается по выручке и по числу чеков`() {
        val byRevenue = sortedUnits(units, SalesSort(SalesOrder.Revenue))
        assertEquals(listOf("Третья", "Первая", "Вторая"), byRevenue.map { it.name })
        val byReceipts = sortedUnits(units, SalesSort(SalesOrder.Receipts))
        assertEquals(listOf("Вторая", "Третья", "Первая"), byReceipts.map { it.name })
    }

    @Test
    fun `нажатие на свой столбец переворачивает порядок, на чужой — переносит его`() {
        val start = SalesSort()
        val turned = start.toggled(SalesOrder.Revenue)
        assertTrue(!turned.descending)
        assertEquals(listOf("Вторая", "Первая", "Третья"), sortedUnits(units, turned).map { it.name })
        val moved = turned.toggled(SalesOrder.Receipts)
        assertEquals(SalesSort(SalesOrder.Receipts, descending = true), moved)
    }

    // --- Срок ---

    @Test
    fun `неделя и месяц считаются от сегодняшнего дня`() {
        val week = span(JournalPeriod.of(JournalSpan.Week, today))
        assertEquals(LocalDate.parse("2026-09-14"), week.from)
        assertEquals(today, week.to)
        val month = span(JournalPeriod.of(JournalSpan.Month, today))
        assertEquals(30, month.days)
    }

    /**
     * «Всё время» уходит кабинету последними 92 сутками — его пределом.
     *
     * Прежде брался год, и кабинет на него всегда отвечал отказом
     * `ANALYTICS_PERIOD_TOO_LONG`: раздел «за всё время» не показывал ничего.
     */
    @Test
    fun `у всего времени границ нет, и сводка берёт предел кабинета`() {
        val all = JournalPeriod.of(JournalSpan.All, today)
        assertNull(all.range)
        val range = span(all)
        assertEquals(LocalDate.parse("2026-06-21"), range.from)
        assertEquals(today, range.to)
        assertEquals(SALES_MOST_DAYS, range.days)
        assertEquals("21.06.2026 — 20.09.2026", salesRangeText(range))
    }

    @Test
    fun `перелистнутый срок уходит в кабинет своими датами`() {
        val earlier = JournalPeriod.of(JournalSpan.Week, today).shiftedBy(-1)
        val filter = span(earlier).filter()
        assertEquals("2026-09-07", filter.from)
        assertEquals("2026-09-13", filter.to)
    }

    private fun span(period: JournalPeriod) =
        SalesSpan.of(period.range?.from, period.range?.to, today)

    // --- Пустой срок ---

    @Test
    fun `срок без единого чека считается пустым, даже если сутки в ответе есть`() {
        val view = SalesView(
            range = SalesSpan(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-07")),
            summary = SalesSummary(),
            days = (1..7).map { SalesDay(date = "2026-09-0$it") },
            hours = emptyList(),
            registers = listOf(SalesUnit(id = "c1", name = "Касса")),
            places = emptyList(),
            delivery = SalesDelivery()
        )
        assertTrue(view.empty)
        assertTrue(!view.copy(summary = SalesSummary(receiptCount = 1)).empty)
        assertTrue(
            !view.copy(summary = SalesSummary(purchaseCount = 1)).empty,
            "срок, в который только скупали у населения, документы за собой оставил"
        )
    }

    @Test
    fun `разность и средний чек выводятся, когда кабинет их не прислал`() {
        val summary = SalesSummary(receiptCount = 4, revenue = sum("400.00"), refunds = sum("100.00"))
        assertEquals(sum("300.00"), summary.net)
        assertEquals(sum("100.00"), summary.average)
    }
}
