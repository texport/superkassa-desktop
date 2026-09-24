package kz.mybrain.superkassa.domain.analytics.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
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
    fun `идущий срок сравнивается с тем же отрезком прошлого`() {
        val answer = runBlocking { ReadSales(analytics) { today }(LocalDate(2026, 9, 17), null) }
        val view = assertNotNull(answer.valueOrNull())
        assertEquals(10, view.previous?.receiptCount, "идущая неделя не сравнена с прошлой")
        assertTrue(analytics.asked.any { it.startsWith("summary") && "from=2026-09-10, to=2026-09-16" in it })
        assertTrue(analytics.asked.any { it.startsWith("sales") && "from=2026-09-17, to=2026-09-23" in it })
    }

    @Test
    fun `открытые смены сети считаются по кассам, как в учёте`() {
        val kkms = listOf(open("a"), open("b"), closed("c"))
        analytics.kkms = { AnalyticsAnswer.Done(KkmMapView(withoutPosition = kkms)) }
        val view = assertNotNull(runBlocking { ReadSales(analytics) { today }(null, null) }.valueOrNull())
        assertEquals(2, view.openShifts)
    }

    @Test
    fun `кабинет не ответил о кассах — сводка есть, числа смен нет`() {
        val view = assertNotNull(runBlocking { ReadSales(analytics) { today }(null, null) }.valueOrNull())
        assertNull(view.openShifts)
        assertEquals(10, view.previous?.receiptCount)
    }

    @Test
    fun `у одной кассы числа смен сети нет и кассы не спрашиваются`() {
        val view = assertNotNull(runBlocking { ReadKkmSales(analytics) { today }("r1", null, null) }.valueOrNull())
        assertNull(view.openShifts)
        assertFalse(analytics.asked.any { it.startsWith("kkms") })
    }

    private fun open(id: String) = AnalyticsKkm(cashRegisterId = id, shiftStatus = "OPEN")

    private fun closed(id: String) = AnalyticsKkm(cashRegisterId = id, shiftStatus = "CLOSED")

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
