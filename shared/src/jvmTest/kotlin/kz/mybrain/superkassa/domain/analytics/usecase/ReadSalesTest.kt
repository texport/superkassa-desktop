package kz.mybrain.superkassa.domain.analytics.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.SALES_MOST_DAYS
import kz.mybrain.superkassa.domain.analytics.model.SalesDelivery
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.model.troubleOrNull
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.presentation.analytics.FakeAnalytics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Сводка срока без модели: какой срок уходит кабинету, когда сравнивается
 * прошлый и как часто читается справочник точек.
 */
class ReadSalesTest {
    private val today = LocalDate.parse("2026-09-23")
    private val analytics = FakeAnalytics().apply {
        sales = { AnalyticsAnswer.Done(FIGURES) }
        summary = { AnalyticsAnswer.Done(SalesSummary(receiptCount = 10)) }
        places = { AnalyticsAnswer.Done(listOf(PlaceAddress("p1", "Магазин", "Алматы, Медеуский, Достык, 10"))) }
    }

    @Test
    fun `идущий срок с прошлым не сравнивается`() {
        val answer = runBlocking { ReadSales(analytics) { today }(LocalDate(2026, 9, 17), null) }
        val view = assertNotNull(answer.valueOrNull())
        assertTrue(view.running)
        assertNull(view.previous)
        assertFalse(analytics.asked.any { it.startsWith("summary") })
        assertTrue(analytics.asked.any { it.startsWith("sales") && "from=2026-09-17, to=2026-09-23" in it })
    }

    @Test
    fun `закончившийся срок сравнивается с прошлым той же длины`() {
        val read = ReadSales(analytics) { today }
        val view = assertNotNull(runBlocking { read(LocalDate(2026, 9, 10), LocalDate(2026, 9, 16)) }.valueOrNull())
        assertEquals(10, view.previous?.receiptCount)
        assertTrue(analytics.asked.any { it.startsWith("summary") && "from=2026-09-03, to=2026-09-09" in it })
    }

    @Test
    fun `точки читаются один раз за вход и заново — после смены владельца`() {
        val read = ReadSales(analytics) { today }
        runBlocking { read(null, null) }
        runBlocking { read(null, null) }
        assertEquals(1, analytics.asked.count { it == "places" })
        read.forget()
        runBlocking { read(null, null) }
        assertEquals(2, analytics.asked.count { it == "places" })
    }

    @Test
    fun `всё время — предел кабинета, а отказ разрезов — помеха всей сводки`() {
        val view = assertNotNull(runBlocking { ReadSales(analytics) { today }(null, null) }.valueOrNull())
        assertEquals(SALES_MOST_DAYS, view.range.days)

        analytics.sales = { AnalyticsAnswer.Troubled(AnalyticsTrouble.Unreachable) }
        val answer = runBlocking { ReadSales(analytics) { today }(null, null) }
        assertEquals(AnalyticsTrouble.Unreachable, answer.troubleOrNull())
    }

    @Test
    fun `сводка кассы отбирает кассу и точек не читает`() {
        val view = runBlocking { ReadKkmSales(analytics) { today }("c7", null, null) }.valueOrNull()
        assertNotNull(view)
        assertTrue(analytics.asked.single { it.startsWith("sales") }.contains("cashRegisterId=c7"))
        assertFalse(analytics.asked.any { it == "places" })
        assertTrue(view.retailPlaces.isEmpty())
    }

    private companion object {
        val FIGURES = SalesFigures(
            summary = SalesSummary(receiptCount = 82),
            days = emptyList(),
            hours = emptyList(),
            registers = emptyList(),
            places = emptyList(),
            delivery = SalesDelivery()
        )
    }
}
