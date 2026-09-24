package kz.mybrain.superkassa.presentation.journal

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.presentation.journal.documents.HistoryView
import kz.mybrain.superkassa.presentation.journal.documents.journalModel
import kz.mybrain.superkassa.presentation.journal.shifts.shiftsModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Журнал и прошлые смены без окна: касса по заказу и держатель входа.
 *
 * Проверяется то, чего экран сам не знает: какой срок спрошен у кассы,
 * что осталось от прежнего срока и как названы отказ и сбой.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val core = FakeCore().apply { on("getDocumentTypes") { emptyList<Any>() } }
    private val signIn = SignIn()
    private val notices = Notices()
    private val asked = mutableListOf<List<Any?>>()

    @BeforeTest
    fun inlineMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("listFiscalDocumentsByPeriod") { args -> listOf(CoreScene.document("d-1")).also { asked += args } }
    }

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun enter(pin: String = CoreScene.PIN, cashier: UserResponse = CoreScene.cashier()) =
        signIn.enter(CoreScene.kkm(), cashier, pin)

    private fun journal() = journalModel(CoreScene.app(core, signIn, notices))

    @Test
    fun `журнал читается у выбранной кассы пином кассира за сегодняшние сутки`() {
        enter()
        val model = journal()

        val state = model.state.value
        assertEquals(listOf("d-1"), state.documents.map { it.id })
        assertEquals(PageOutcome.page(more = false), state.page)
        assertFalse(state.loading)
        val args = asked.last()
        assertEquals("kkm-1", args[0])
        assertEquals(state.period.range?.fromMillis(), args[1])
        assertEquals(state.period.range?.toMillis(), args[2])
        assertTrue((args[3] as Int) in 1..500, "страница больше, чем отдаёт касса: ${args[3]}")
        assertEquals(0, args[4])
        assertEquals(CoreScene.PIN, args[5])
    }

    /** Кассира нет — спрашивать некого, и «документов нет» было бы утверждением о кассе, которую никто не назвал. */
    @Test
    fun `без вошедшего кассира журнал не прочитан, а не пуст`() {
        val model = journal()

        assertFalse(model.state.value.page.read)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun `новый срок читается с начала, отбор остаётся`() {
        enter()
        val model = journal()
        model.filter(JournalQuery(search = "4500"))
        val week = JournalPeriod.of(JournalSpan.Week)

        model.choose(week)

        assertEquals(week, model.state.value.period)
        assertEquals("4500", model.state.value.query.search)
        assertEquals(week.range?.fromMillis(), asked.last()[1])
        assertEquals(0, asked.last()[4], "новый срок дочитывается со смещения прошлого")
    }

    /** У «всего времени» границ нет: касса спрашивается с начала счёта и до конца сегодняшних суток. */
    @Test
    fun `всё время спрашивается с начала счёта`() {
        enter()
        val model = journal()

        model.choose(JournalPeriod.of(JournalSpan.All))

        assertEquals(0L, asked.last()[1])
        assertTrue((asked.last()[2] as Long) > System.currentTimeMillis(), "сегодняшний день обрезан")
    }

    @Test
    fun `отказ кассы — её словами, срок не прочитан`() {
        core.refuse("listFiscalDocumentsByPeriod", "PIN_LOCKED", ru = "Касса заблокирована, повторите через 2 мин.")
        enter()
        val model = journal()

        assertFalse(model.state.value.page.read)
        assertEquals(Message.Refusal("Касса заблокирована, повторите через 2 мин.", "PIN_LOCKED"), notices.last)
    }

    @Test
    fun `сбой кассы назван сбоем`() {
        core.on("listFiscalDocumentsByPeriod") { error("database is locked") }
        enter()
        val model = journal()

        assertFalse(model.state.value.page.read)
        assertIs<Message.Failed>(notices.last)
    }

    /** Сменился кассир — прочитанное прежним пином не остаётся на экране нового. */
    @Test
    fun `смена кассира перечитывает журнал его пином`() {
        enter()
        val model = journal()
        model.show(HistoryView.Shifts)

        enter(pin = "4821", cashier = UserResponse(userId = "u-2", name = "Дана Жумабаева", role = UserRole.CASHIER))

        assertEquals("4821", asked.last()[5])
        assertEquals(HistoryView.Shifts, model.state.value.view, "взгляд на журнал сброшен сменой кассира")
    }

    @Test
    fun `смена открывается и читает свои документы`() {
        val shift = ShiftResponse(id = "s-1", kkmId = "kkm-1", shiftNo = 7, status = ShiftStatus.CLOSED, openedAt = 0)
        core.on("listShifts") { listOf(shift) }
        core.on("listShiftDocuments") { args -> listOf(CoreScene.document("d-7")).also { asked += args } }
        enter()
        val model = shiftsModel(CoreScene.app(core, signIn, notices))
        assertEquals(listOf("s-1"), model.state.value.shifts.map { it.id })
        assertEquals(PageOutcome.page(more = false), model.state.value.page)

        model.open(shift)

        val state = model.state.value
        assertEquals("s-1", asked.last()[1])
        assertEquals(listOf("d-7"), state.documents.map { it.id })
        assertTrue(state.documentsPage.read && !state.opening)

        model.open(null)
        assertTrue(model.state.value.documents.isEmpty(), "документы прошлой смены остались под списком смен")
    }

    @Test
    fun `отказ в документах смены — не пустая смена`() {
        val shift = ShiftResponse(id = "s-1", kkmId = "kkm-1", shiftNo = 7, status = ShiftStatus.CLOSED, openedAt = 0)
        core.on("listShifts") { listOf(shift) }
        core.refuse("listShiftDocuments", "KKM_BLOCKED", ru = "Касса заблокирована")
        enter()
        val model = shiftsModel(CoreScene.app(core, signIn, notices))

        model.open(shift)

        assertFalse(model.state.value.documentsPage.read)
        assertFalse(model.state.value.opening)
        assertEquals(Message.Refusal("Касса заблокирована", "KKM_BLOCKED"), notices.last)
    }
}
