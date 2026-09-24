package kz.mybrain.superkassa.presentation.analytics.exchange

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddresses
import kz.mybrain.superkassa.presentation.analytics.FakeAnalytics
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Вкладка адресов обмена без окна: помехи своими словами, поиск и отбор
 * по уже прочитанному — без новых вопросов кабинету.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsExchangeViewModelTest {
    private val analytics = FakeAnalytics()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `отказ кабинета приходит его словами, а невыложенный раздел — своим случаем`() {
        val model = AnalyticsExchangeViewModel(ExchangeCases(analytics))
        model.follow(OWNER)
        assertEquals(AnalyticsTrouble.NotDeployed, model.state.value.reading.trouble)

        analytics.exchange = { AnalyticsAnswer.Troubled(AnalyticsTrouble.Refused("Адреса выданы не этой компании")) }
        model.refresh()

        assertEquals(AnalyticsTrouble.Refused("Адреса выданы не этой компании"), model.state.value.reading.trouble)
        assertTrue(model.state.value.rows.isEmpty())
    }

    @Test
    fun `поиск и отбор по кассе работают по уже прочитанному`() {
        analytics.exchange = {
            AnalyticsAnswer.Done(
                ExchangeAddresses(
                    addresses = listOf(
                        ExchangeAddress("c1", internalName = "Касса у входа", address = "10.0.0.1"),
                        ExchangeAddress("c2", internalName = "Касса в зале", address = "10.0.0.2"),
                        ExchangeAddress("c2", internalName = "Касса в зале", address = "10.0.0.3")
                    )
                )
            )
        }
        val model = AnalyticsExchangeViewModel(ExchangeCases(analytics))
        model.follow(OWNER)

        model.pick("c2")
        assertEquals(listOf("10.0.0.2", "10.0.0.3"), model.state.value.rows.map { it.address })
        model.search("0.3")
        assertEquals(listOf("10.0.0.3"), model.state.value.rows.map { it.address })
        assertEquals(1, analytics.asked.size, "отбор ходил в кабинет")
    }

    private companion object {
        /** Отметка вошедшего владельца: самого доступа модели не видят. */
        const val OWNER = "owner-1"
    }
}
