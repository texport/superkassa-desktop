package kz.mybrain.superkassa.presentation.journal

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.presentation.common.document.JournalEmpty
import kz.mybrain.superkassa.presentation.common.document.journalState
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsActions
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsScreen
import kz.mybrain.superkassa.presentation.journal.shifts.ShiftsUiState
import kz.mybrain.superkassa.presentation.journal.shifts.shiftDocumentsState
import kz.mybrain.superkassa.presentation.journal.shifts.shiftsState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Непрочитанный список не выдаётся за пустой.
 *
 * «Документов нет», «смен нет», «подходящих чеков-оснований нет» —
 * утверждения о кассе, и говорить их можно только вслед за ответом узла.
 * Прежде чтение отвечало одним `Boolean` — «есть ли ещё страница», — и тем
 * же `false` отвечало на молчание узла: кассир при покупателе с чеком
 * в руках читал отказ узла как отказ в возврате.
 */
class JournalUnreadTest {

    private val texts = textsOf(Language.Ru).journal

    @Test
    fun `журнал называет непрочитанный срок бедой чтения, а не пустым сроком`() {
        val journal = texts.history
        val empty = JournalEmpty(journal.emptyDay, journal.emptyDayHint)

        val unread = journalState(journal, empty, rows = 0, shown = 0, loading = false, unread = true) {}
        val read = journalState(journal, empty, rows = 0, shown = 0, loading = false, unread = false, onRetry = null)

        assertEquals(ScreenState.Trouble(journal.unread, journal.unreadHint), withoutRetry(unread))
        assertEquals(ScreenState.Empty(iconOf(read), empty.title, empty.hint), read)
    }

    /** Прочитанные строки остаются на экране: неудача дочитывания их не убирает. */
    @Test
    fun `неудача дочитывания не убирает с экрана уже прочитанные строки`() {
        val journal = texts.history
        val empty = JournalEmpty(journal.emptyDay, journal.emptyDayHint)

        val state = journalState(journal, empty, rows = 42, shown = 42, loading = false, unread = true) {}

        assertEquals(ScreenState.Ready, state)
    }

    @Test
    fun `прошлые смены называют непрочитанное бедой чтения, а не отсутствием смен`() {
        val journal = texts.shifts

        val unread = shiftsState(journal, shifts = 0, loading = false, page = PageOutcome.unread) {}
        val none = shiftsState(journal, shifts = 0, loading = false, page = PageOutcome.page(false)) {}

        assertEquals(ScreenState.Trouble(journal.unread, journal.unreadHint), withoutRetry(unread))
        assertEquals(ScreenState.Empty(iconOf(none), journal.none, journal.noneHint), none)
    }

    /**
     * Та же разница — на самом экране, а не только в разборе состояний.
     *
     * Кассир приходит сюда за Z-отчётом позавчерашней смены, и «смен нет»
     * над молчащим узлом он читает как утрату смены.
     */
    @Test
    fun `экран прошлых смен рисует непрочитанное иначе, чем кассу без смен`() {
        val none = KassaScene.shot("shifts-none") {
            ShiftsScreen(ShiftsUiState(loading = false, page = PageOutcome.page(false)), NO_SHIFTS, NO_PRINT)
        }
        val unread = KassaScene.shot("shifts-unread") {
            ShiftsScreen(ShiftsUiState(loading = false, page = PageOutcome.unread), NO_SHIFTS, NO_PRINT)
        }

        assertTrue(none.isNotEmpty() && unread.isNotEmpty(), "экран не собрался")
        assertTrue(!none.contentEquals(unread), "«смен нет» и «прочитать не удалось» на экране неразличимы")
    }

    @Test
    fun `документы смены называют непрочитанное бедой чтения, а не пустой сменой`() {
        val journal = texts.shifts

        val unread = shiftDocumentsState(journal, documents = 0, loading = false, page = PageOutcome.unread) {}
        val none = shiftDocumentsState(journal, documents = 0, loading = false, page = PageOutcome.page(false)) {}

        assertEquals(
            ScreenState.Trouble(journal.documentsUnread, journal.documentsUnreadHint),
            withoutRetry(unread)
        )
        assertEquals(ScreenState.Empty(iconOf(none), journal.emptyDocuments, journal.emptyDocumentsHint), none)
    }

    /**
     * Та же разница — на самом экране смены, а не только в разборе состояний.
     *
     * Смена открывается нажатием на её строку, и документы касса отдаёт
     * отдельным обращением: молчание этого обращения выглядело как смена,
     * в которой не пробито ни чека. Что нажатие открывает смену и читает
     * её документы — проверка модели смен.
     */
    @Test
    fun `открытая смена рисует непрочитанные документы иначе, чем смену без чеков`() {
        val shift = ShiftResponse(
            id = "s-1",
            kkmId = "kkm-1",
            shiftNo = 1,
            status = ShiftStatus.CLOSED,
            openedAt = 0,
            closeDocumentId = "d-close"
        )
        val opened = ShiftsUiState(listOf(shift), PageOutcome.page(false), loading = false, opened = shift)
        val empty = openedShift(opened.copy(documentsPage = PageOutcome.page(false)))
        val unread = openedShift(opened.copy(documentsPage = PageOutcome.unread))

        assertTrue(empty.isNotEmpty() && unread.isNotEmpty(), "экран не собрался")
        assertTrue(
            !empty.contentEquals(unread),
            "«документов нет» и «прочитать не удалось» на экране смены неразличимы"
        )
    }

    /** Кадр экрана открытой смены. */
    private fun openedShift(state: ShiftsUiState): ByteArray =
        RenderProbe(width = KassaScene.WIDE, height = KassaScene.TALL) {
            ShiftsScreen(state, NO_SHIFTS, NO_PRINT)
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }

    /** Кнопка повтора в сравнении не участвует: сравниваются слова, а не замыкания. */
    private fun withoutRetry(state: ScreenState): ScreenState =
        (state as ScreenState.Trouble).copy(onRetry = null)

    /** Значок пустого состояния берётся у него же: проверяются слова, а не картинка. */
    private fun iconOf(state: ScreenState) = (state as ScreenState.Empty).icon

    private companion object {
        /** Смены без действий: снимок нажимать не будет. */
        val NO_SHIFTS = object : ShiftsActions {}

        /** Печать без действий: снимок нажимать не будет. */
        val NO_PRINT = object : PrintActions {}

        /** Сколько кадров даётся чтению, чтобы доехать до экрана. */
        const val SETTLE = 40
    }
}
