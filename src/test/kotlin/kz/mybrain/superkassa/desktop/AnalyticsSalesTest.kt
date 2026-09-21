package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.SalesDay
import kz.mybrain.superkassa.desktop.server.cabinet.SalesDelivery
import kz.mybrain.superkassa.desktop.server.cabinet.SalesHour
import kz.mybrain.superkassa.desktop.server.cabinet.SalesPayments
import kz.mybrain.superkassa.desktop.server.cabinet.SalesSummary
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.ui.analytics.SalesOrder
import kz.mybrain.superkassa.desktop.ui.analytics.SalesSort
import kz.mybrain.superkassa.desktop.ui.analytics.SalesView
import kz.mybrain.superkassa.desktop.ui.analytics.axisStep
import kz.mybrain.superkassa.desktop.ui.analytics.barAt
import kz.mybrain.superkassa.desktop.ui.analytics.barShare
import kz.mybrain.superkassa.desktop.ui.analytics.dayBars
import kz.mybrain.superkassa.desktop.ui.analytics.hourBars
import kz.mybrain.superkassa.desktop.ui.analytics.SalesColumn
import kz.mybrain.superkassa.desktop.ui.analytics.SalesRows
import kz.mybrain.superkassa.desktop.ui.analytics.salesColumnTitle
import kz.mybrain.superkassa.desktop.ui.analytics.salesColumns
import kz.mybrain.superkassa.desktop.ui.analytics.salesFilter
import kz.mybrain.superkassa.desktop.ui.analytics.salesSortOrder
import kz.mybrain.superkassa.desktop.ui.analytics.salesRange
import kz.mybrain.superkassa.desktop.ui.analytics.salesRangeText
import kz.mybrain.superkassa.desktop.ui.analytics.salesShares
import kz.mybrain.superkassa.desktop.ui.analytics.sortedUnits
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalRange
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import java.math.BigDecimal
import java.time.LocalDate
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

    private val texts = analyticsTexts(Language.Ru).sales
    private val enums = stringsOf(Language.Ru).enums
    private val today = LocalDate.parse("2026-09-20")

    private fun sum(value: String) = BigDecimal(value)

    // --- Виды расчётов ---

    @Test
    fun `ноль не становится долей, а остальные складываются в целое`() {
        val shares = salesShares(
            SalesPayments(cash = sum("250.00"), card = sum("750.00"), mobile = BigDecimal.ZERO),
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
        val week = salesRange(JournalPeriod.of(JournalSpan.Week, today), today)
        assertEquals(LocalDate.parse("2026-09-14"), week.from)
        assertEquals(today, week.to)
        val month = salesRange(JournalPeriod.of(JournalSpan.Month, today), today)
        assertEquals(30L, month.days)
    }

    @Test
    fun `у всего времени границ нет, и сводка берёт последний год`() {
        val all = JournalPeriod.of(JournalSpan.All, today)
        assertNull(all.range)
        val range = salesRange(all, today)
        assertEquals(LocalDate.parse("2025-09-21"), range.from)
        assertEquals(today, range.to)
        assertEquals("21.09.2025 — 20.09.2026", salesRangeText(all, today))
    }

    @Test
    fun `перелистнутый срок уходит в кабинет своими датами`() {
        val earlier = JournalPeriod.of(JournalSpan.Week, today).shiftedBy(-1)
        val filter = salesFilter(earlier, today = today)
        assertEquals(LocalDate.parse("2026-09-07"), filter.from)
        assertEquals(LocalDate.parse("2026-09-13"), filter.to)
        assertTrue(filter.query().startsWith("?from=2026-09-07&to=2026-09-13"), filter.query())
    }

    // --- Столбики ---

    @Test
    fun `суток столько, сколько в сроке, а часов всегда двадцать четыре`() {
        val one = JournalRange(LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-19"))
        val days = dayBars(listOf(SalesDay(date = "2026-09-19", receiptCount = 2, revenue = sum("10.00"))), one, texts)
        assertEquals("19.09", days.single().label)
        assertTrue(days.single().caption.contains("19.09.2026"), days.single().caption)

        val hours = hourBars(listOf(SalesHour(hour = 14, receiptCount = 9, revenue = sum("90.00"))), texts)
        assertEquals(24, hours.size)
        assertEquals(sum("90.00"), hours[14].value)
        assertEquals(BigDecimal.ZERO, hours[0].value)
        assertTrue(hours[14].caption.startsWith("14:00"), hours[14].caption)
    }

    @Test
    fun `пустые сутки срока достраиваются нулём на своих местах`() {
        val week = JournalRange(LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-20"))
        val bars = dayBars(
            listOf(
                SalesDay(date = "2026-09-15", receiptCount = 4, revenue = sum("400.00")),
                SalesDay(date = "2026-09-17", receiptCount = 1, revenue = sum("100.00")),
                SalesDay(date = "2026-09-20", receiptCount = 9, revenue = sum("900.00"))
            ),
            week,
            texts
        )
        assertEquals(7, bars.size, bars.joinToString { it.label })
        assertEquals(listOf("14.09", "15.09", "16.09", "17.09", "18.09", "19.09", "20.09"), bars.map { it.label })
        assertEquals(BigDecimal.ZERO, bars.first().value)
        assertEquals(sum("400.00"), bars[1].value)
        assertEquals(BigDecimal.ZERO, bars[2].value)
        assertEquals(sum("900.00"), bars.last().value)
    }

    @Test
    fun `сутки вне срока в ряд не попадают, а порядок остаётся временным`() {
        val twoDays = JournalRange(LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-20"))
        val bars = dayBars(
            listOf(
                SalesDay(date = "2026-09-20", receiptCount = 2, revenue = sum("20.00")),
                SalesDay(date = "2026-08-01", receiptCount = 7, revenue = sum("70.00")),
                SalesDay(date = "2026-09-19", receiptCount = 1, revenue = sum("10.00"))
            ),
            twoDays,
            texts
        )
        assertEquals(listOf("19.09", "20.09"), bars.map { it.label })
        assertEquals(sum("10.00"), bars.first().value)
    }

    @Test
    fun `пустые сутки подписаны нулём чеков, а не прочерком`() {
        val oneDay = JournalRange(LocalDate.parse("2026-09-18"), LocalDate.parse("2026-09-18"))
        val caption = dayBars(emptyList(), oneDay, texts).single().caption
        assertTrue(caption.contains("18.09.2026"), caption)
        assertTrue(caption.contains("${texts.receipts}: 0"), caption)
    }

    // --- Столбцы таблиц ---

    @Test
    fun `у таблицы точек нет столбцов кассы`() {
        val places = salesColumns(SalesRows.Places)
        assertTrue(SalesColumn.RegistrationNumber !in places, places.joinToString())
        assertTrue(SalesColumn.RetailPlace !in places, places.joinToString())
        assertEquals(salesColumns(SalesRows.Registers).size - 2, places.size)
    }

    @Test
    fun `у таблицы касс столбцы кассы на месте и подписаны по-своему`() {
        val whole = analyticsTexts(Language.Ru)
        val registers = salesColumns(SalesRows.Registers)
        assertTrue(SalesColumn.RegistrationNumber in registers)
        assertEquals(whole.sales.colName, salesColumnTitle(SalesColumn.Name, SalesRows.Registers, whole))
        assertEquals(whole.retailPlace, salesColumnTitle(SalesColumn.Name, SalesRows.Places, whole))
    }

    @Test
    fun `выстраивают таблицу только числовые столбцы`() {
        assertEquals(SalesOrder.Revenue, salesSortOrder(SalesColumn.Revenue))
        assertEquals(SalesOrder.Receipts, salesSortOrder(SalesColumn.Receipts))
        assertNull(salesSortOrder(SalesColumn.Name))
        assertNull(salesSortOrder(SalesColumn.LastContact))
    }

    @Test
    fun `высота столбика — доля от наибольшего, а ноль не рисуется`() {
        assertEquals(1f, barShare(sum("100"), sum("100")))
        assertEquals(0.5f, barShare(sum("50"), sum("100")))
        assertEquals(0f, barShare(BigDecimal.ZERO, sum("100")))
        assertEquals(0f, barShare(sum("10"), BigDecimal.ZERO))
    }

    @Test
    fun `под указателем всегда ровно один столбик, а за полотном — ни одного`() {
        assertEquals(0, barAt(x = 1f, width = 100, count = 10))
        assertEquals(9, barAt(x = 99f, width = 100, count = 10))
        assertEquals(9, barAt(x = 100f, width = 100, count = 10))
        assertNull(barAt(x = -1f, width = 100, count = 10))
        assertNull(barAt(x = 101f, width = 100, count = 10))
        assertNull(barAt(x = 10f, width = 100, count = 0))
    }

    @Test
    fun `ось подписывается не чаще, чем подписи помещаются`() {
        assertEquals(1, axisStep(7))
        assertEquals(1, axisStep(8))
        assertEquals(4, axisStep(30))
        assertEquals(3, axisStep(24))
    }

    // --- Пустой срок ---

    @Test
    fun `срок без единого чека считается пустым, даже если сутки в ответе есть`() {
        val view = SalesView(
            range = JournalRange(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-07")),
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
