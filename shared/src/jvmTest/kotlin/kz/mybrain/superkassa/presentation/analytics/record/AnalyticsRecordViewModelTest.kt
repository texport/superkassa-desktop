package kz.mybrain.superkassa.presentation.analytics.record

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.presentation.analytics.FakeAnalytics
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Вкладка учёта без окна: что спрошено у кабинета и что вышло.
 *
 * Модель живёт, пока открыто окно: тот же владелец — прочитанное не
 * перечитывается; вошёл другой или вышел этот — ответ прежнего пропадает.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsRecordViewModelTest {
    private val analytics = FakeAnalytics()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `учёт спрашивает кассы по адресу точки и не перечитывает их на возврате`() {
        analytics.kkms = { AnalyticsAnswer.Done(KkmMapView(placed = listOf(AnalyticsKkm("c1")))) }
        val model = AnalyticsRecordViewModel(RecordCases(analytics))

        model.follow(OWNER)
        model.follow(OWNER)

        assertEquals(listOf("kkms ${PositionSource.RetailPlaceAddress}"), analytics.asked)
        assertEquals(listOf("c1"), model.state.value.value?.kkms?.map { it.cashRegisterId })
        assertNull(model.state.value.trouble)
    }

    @Test
    fun `вошёл другой владелец — прежний ответ пропадает`() {
        analytics.kkms = { AnalyticsAnswer.Done(KkmMapView(placed = listOf(AnalyticsKkm("c1")))) }
        val model = AnalyticsRecordViewModel(RecordCases(analytics))
        model.follow(OWNER)

        analytics.kkms = { AnalyticsAnswer.Troubled(AnalyticsTrouble.Unreachable) }
        model.follow("other-owner")

        assertNull(model.state.value.value, "касса прежнего владельца осталась на экране")
        assertEquals(AnalyticsTrouble.Unreachable, model.state.value.trouble)
    }

    @Test
    fun `вышел из кабинета — вкладка пуста и никуда не ходит`() {
        analytics.kkms = { AnalyticsAnswer.Done(KkmMapView(placed = listOf(AnalyticsKkm("c1")))) }
        val model = AnalyticsRecordViewModel(RecordCases(analytics))
        model.follow(OWNER)

        model.follow(null)
        model.refresh()

        assertNull(model.state.value.value)
        assertEquals(1, analytics.asked.size)
    }

    private companion object {
        /** Отметка вошедшего владельца: самого доступа модели не видят. */
        const val OWNER = "owner-1"
    }
}
