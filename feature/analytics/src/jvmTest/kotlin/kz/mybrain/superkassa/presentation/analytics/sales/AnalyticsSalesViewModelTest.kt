package kz.mybrain.superkassa.presentation.analytics.sales

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.SalesDelivery
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.port.FakeAnalytics
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Торговая сводка без окна: какой срок уходит кабинету и что из ответа
 * попадает на экран.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsSalesViewModelTest {
    private val today = LocalDate.parse("2026-09-23")
    private val analytics = FakeAnalytics().apply {
        sales = { AnalyticsAnswer.Done(FIGURES) }
        summary = { AnalyticsAnswer.Done(SalesSummary(receiptCount = 10, revenue = 100_000L)) }
        places = { AnalyticsAnswer.Done(listOf(PlaceAddress("p1", "Магазин", "Алматы, Медеуский, Достык, 10"))) }
    }

    /** Модель сводки на подставном кабинете; сегодня — [today]. */
    private fun model(register: String? = null) =
        AnalyticsSalesViewModel(SalesCases(analytics) { today }, register)

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `неделя до сегодня уходит кабинету и сравнивается с прошлой неделей`() {
        val model = model()

        model.follow(OWNER)

        val view = assertNotNull(model.state.value.reading.value)
        assertTrue(analytics.asked.any { it.startsWith("sales") && "from=2026-09-17, to=2026-09-23" in it })
        assertEquals(10, view.previous?.receiptCount, "сводка по умолчанию без сравнения")
        assertTrue(analytics.asked.any { it.startsWith("summary") && "from=2026-09-10, to=2026-09-16" in it })
        assertEquals(listOf("p1"), view.retailPlaces.map { it.id })
    }

    @Test
    fun `закончившийся срок сравнивается с прошлым, а точки читаются один раз`() {
        val model = model()
        model.follow(OWNER)

        model.choose(JournalPeriod.of(JournalSpan.Week, today).shiftedBy(-1))

        val view = assertNotNull(model.state.value.reading.value)
        assertEquals(10, view.previous?.receiptCount)
        assertTrue(analytics.asked.any { it.startsWith("summary") && "from=2026-09-03, to=2026-09-09" in it })
        assertEquals(1, analytics.asked.count { it.startsWith("places") }, "справочник точек перечитан")
    }

    @Test
    fun `всё время уходит кабинету его пределом`() {
        val model = model()
        model.follow(OWNER)

        model.choose(JournalPeriod.of(JournalSpan.All, today))

        assertTrue(analytics.asked.last { it.startsWith("sales") }.contains("from=2026-06-24, to=2026-09-23"))
        assertNull(model.state.value.reading.trouble)
    }

    @Test
    fun `отказ прошлого срока сводку не роняет, отказ самой сводки — роняет`() {
        analytics.summary = { AnalyticsAnswer.Troubled(AnalyticsTrouble.Unreachable) }
        val model = model()
        model.follow(OWNER)
        model.choose(JournalPeriod.of(JournalSpan.Week, today).shiftedBy(-1))
        assertNotNull(model.state.value.reading.value, "сводка пропала из-за прошлого срока")

        analytics.sales = { AnalyticsAnswer.Troubled(AnalyticsTrouble.Refused("Срок не тот")) }
        model.refresh()

        assertNull(model.state.value.reading.value)
        assertEquals(AnalyticsTrouble.Refused("Срок не тот"), model.state.value.reading.trouble)
    }

    @Test
    fun `окно кассы спрашивает сводку по кассе и точек не читает`() {
        val model = model(register = "c7")

        model.follow(OWNER)

        assertTrue(analytics.asked.single { it.startsWith("sales") }.contains("cashRegisterId=c7"))
        assertFalse(analytics.asked.any { it.startsWith("places") })
    }

    private companion object {
        const val OWNER = "owner-access"
        val FIGURES = SalesFigures(
            summary = SalesSummary(receiptCount = 82, revenue = 6_182_500L),
            days = emptyList(),
            hours = emptyList(),
            registers = emptyList(),
            places = emptyList(),
            delivery = SalesDelivery()
        )
    }
}
