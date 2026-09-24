package kz.mybrain.superkassa.presentation.shift.dashboard

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.shift.ReportResponse
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
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Главный экран без окна: смена со слов кассы и действия над ней.
 *
 * Состояние смены — только то, что касса назвала: у кассы, снятой с учёта,
 * смена открыта, а документы касса отдать отказывается, и экран обязан
 * показать открытую смену с непрочитанными документами, а не пустую.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val signIn = SignIn()
    private val notices = Notices()
    private val texts = textsOf(Language.Ru).common
    private val blocked = CoreScene.kkm(state = "BLOCKED", blockReasonCode = 1015)

    private fun model(core: FakeCore) = dashboardModel(CoreScene.app(core, signIn, notices))

    private fun enter(admin: Boolean = true) = signIn.enter(CoreScene.kkm(), CoreScene.cashier(admin), CoreScene.PIN)

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `вход читает смену, документы и наличные в тиынах`() {
        val sale = CoreScene.document("d-1", amount = 150_000)
        val model = model(DashboardScene.core(documents = listOf(sale), cash = 125_000))

        enter()

        val state = model.state.value
        assertEquals(ShiftState.Open, state.shift)
        assertEquals(7L, state.shiftNumber)
        assertTrue(state.documentsRead)
        assertEquals(listOf("d-1"), state.documents.map { it.id })
        assertEquals(125_000L, state.cashInDrawer)
        assertNull(notices.last)
    }

    @Test
    fun `открытая смена показывается открытой, хотя документы касса не отдаёт`() {
        val core = DashboardScene.core(kkm = blocked).apply { refuse("listShiftDocuments", "KKM_BLOCKED") }
        val model = model(core)

        enter()

        val state = model.state.value
        assertEquals(ShiftState.Open, state.shift)
        assertTrue(state.blocked)
        assertFalse(state.documentsRead, "непрочитанный список документов объявлен прочитанным")
        assertTrue(state.documents.isEmpty())
    }

    @Test
    fun `закрытая смена показывается закрытой`() {
        val model = model(DashboardScene.core(shift = null))

        enter()

        assertEquals(ShiftState.Closed, model.state.value.shift)
    }

    /** Касса о смене не ответила — модель не называет состояние за неё. */
    @Test
    fun `состояние смены, о котором касса молчит, остаётся неизвестным`() {
        val core = DashboardScene.core().apply { on("getLocalOpenShift") { error("database is closed") } }
        val model = model(core)

        enter()

        assertEquals(ShiftState.Unknown, model.state.value.shift)
        assertIs<Message.Failed>(notices.last)
    }

    @Test
    fun `прочитанный пустой список остаётся прочитанным`() {
        val model = model(DashboardScene.core(documents = emptyList()))

        enter()

        assertTrue(model.state.value.documentsRead, "пустой ответ кассы объявлен непрочитанным")
    }

    /** Итог действия — словами кассира и с тем, что стало с отчётом в БФД. */
    @Test
    fun `X-отчёт объявляется с доставкой в БФД`() {
        val core = DashboardScene.core().apply {
            on("createReport") { ReportResponse(documentId = "x-1", deliveryStatus = DeliveryStatus.ONLINE_OK) }
        }
        val model = model(core)
        enter()

        model.xReport()

        assertEquals(Message.Done("${texts.dashboard.xReportDone}: ${texts.common.deliveredToOfd}"), notices.last)
        assertFalse(model.state.value.busy)
        assertEquals(2, core.calls.count { it == "getLocalOpenShift" }, "после отчёта смена не перечитана")
    }

    @Test
    fun `отказ закрыть смену — словами кассы, кнопки снова свободны`() {
        val core = DashboardScene.core().apply {
            refuse("closeShift", "SHIFT_LONGER_THAN_DAY", ru = "Смена длится больше суток")
        }
        val model = model(core)
        enter()

        model.closeShift()

        assertEquals(Message.Refusal("Смена длится больше суток", "SHIFT_LONGER_THAN_DAY"), notices.last)
        assertFalse(model.state.value.busy)
    }

    /** Итог действия переживает перечитывание: кассир спрашивал, что сделала кнопка. */
    @Test
    fun `беда перечитывания не перебивает итог действия`() {
        val core = DashboardScene.core(shift = null).apply {
            on("openShift") { CoreScene.openShift() }
            refuse("listCounters", "KKM_BLOCKED")
        }
        val model = model(core)
        enter()
        notices.clear()

        model.openShift()

        assertEquals(Message.Done(texts.dashboard.shiftOpened), notices.last)
    }

    @Test
    fun `смена кассира сбрасывает прочитанное`() {
        val model = model(DashboardScene.core(documents = listOf(CoreScene.document("d-1"))))
        enter()

        signIn.signOut()

        val state = model.state.value
        assertEquals(ShiftState.Unknown, state.shift)
        assertTrue(state.documents.isEmpty())
        assertEquals("kkm-1", state.kkm?.kkmId, "касса рабочего места ушла вместе с кассиром")
    }
}
