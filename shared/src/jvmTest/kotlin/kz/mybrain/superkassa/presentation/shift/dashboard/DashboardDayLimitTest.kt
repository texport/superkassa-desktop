package kz.mybrain.superkassa.presentation.shift.dashboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Предел суток смены называет касса, а не главный экран.
 *
 * Экран считал сутки сам — от открытия смены и по своим часам — и писал
 * «смена открыта дольше суток» даже над закрытой сменой. Касса считает
 * предел от первого платёжного документа и по нему же отказывает в чеке;
 * экран показывает её ответ.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardDayLimitTest {
    private val signIn = SignIn()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun read(core: FakeCore) = dashboardModel(CoreScene.app(core, signIn)).also {
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)
    }

    @Test
    fun `пройденный предел берётся у кассы`() {
        val shift = CoreScene.openShift().copy(dayLimitAt = LIMIT, dayLimitExceeded = true)

        val state = read(DashboardScene.core(shift = shift)).state.value

        assertEquals(ShiftState.Open, state.shift)
        assertEquals(LIMIT, state.dayLimitAt)
        assertTrue(state.dayLimitExceeded)
    }

    @Test
    fun `смена открыта давно, а предел не пройден — касса так и сказала`() {
        val shift = CoreScene.openShift(openedAt = LIMIT - 3 * DAY).copy(dayLimitAt = LIMIT, dayLimitExceeded = false)

        val state = read(DashboardScene.core(shift = shift)).state.value

        assertFalse(state.dayLimitExceeded, "экран досчитал сутки сам, мимо кассы")
    }

    @Test
    fun `у закрытой смены предела нет`() {
        val state = read(DashboardScene.core(shift = null)).state.value

        assertEquals(ShiftState.Closed, state.shift)
        assertNull(state.dayLimitAt)
        assertFalse(state.dayLimitExceeded)
    }

    private companion object {
        const val LIMIT = 1_789_000_000_000L
        const val DAY = 24L * 60 * 60 * 1000
    }
}
