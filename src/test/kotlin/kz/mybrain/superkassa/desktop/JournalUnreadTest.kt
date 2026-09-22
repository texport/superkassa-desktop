package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.PAGE
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.JournalEmpty
import kz.mybrain.superkassa.desktop.ui.history.PageOutcome
import kz.mybrain.superkassa.desktop.ui.history.PastShiftsView
import kz.mybrain.superkassa.desktop.ui.history.Shift
import kz.mybrain.superkassa.desktop.ui.history.journalState
import kz.mybrain.superkassa.desktop.ui.history.loadDay
import kz.mybrain.superkassa.desktop.ui.history.shiftDocumentsState
import kz.mybrain.superkassa.desktop.ui.history.shiftsState
import kz.mybrain.superkassa.desktop.ui.returns.ReturnKind
import kz.mybrain.superkassa.desktop.ui.returns.basisState
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    private val texts = journalTexts(Language.Ru)

    private fun receipt(no: Long) = Document(
        id = "d-$no",
        docNo = no,
        printedDocumentNumber = no,
        docType = "SALE",
        totalAmount = no * 1_000,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `узел не ответил — день остаётся непрочитанным`() = runBlocking {
        val session = KassaScene.session("unread-silent", journalAnswered = false)
        val into = mutableListOf<Document>()

        val outcome = loadDay(session, WHAT, LocalDate.now(), into)

        assertEquals(PageOutcome.unread, outcome)
        assertTrue(into.isEmpty())
        assertNotNull(session.lastMessage, "молчание узла обязано остаться сказанным")
    }

    @Test
    fun `кассы не выбрано — читать некого, и день тоже непрочитан`() = runBlocking {
        val session = KassaScene.session("unread-no-kkm", kkm = null)

        val outcome = loadDay(session, WHAT, LocalDate.now(), mutableListOf())

        assertEquals(PageOutcome.unread, outcome)
    }

    @Test
    fun `узел ответил пустым днём — день прочитан, и читать дальше нечего`() = runBlocking {
        val session = KassaScene.session("unread-empty")
        val into = mutableListOf<Document>()

        val outcome = loadDay(session, WHAT, LocalDate.now(), into)

        assertEquals(PageOutcome.page(more = false), outcome)
        assertTrue(into.isEmpty(), "пустой день — это прочитанный пустой день")
    }

    @Test
    fun `полная страница означает, что день читается дальше`() = runBlocking {
        val day = (1L..PAGE).map { receipt(it) }
        val session = KassaScene.session("unread-full", journal = day)
        val into = mutableListOf<Document>()

        val outcome = loadDay(session, WHAT, LocalDate.now(), into)

        assertEquals(PageOutcome.page(more = true), outcome)
        assertEquals(PAGE, into.size)
    }

    @Test
    fun `возврат называет непрочитанный день бедой чтения, а не отсутствием оснований`() {
        val session = KassaScene.session("unread-basis", shift = KassaScene.openShift())
        val journal = texts.returns

        val unread = basisState(session, journal, ReturnKind.Sell, false, false, PageOutcome.unread) {}
        val empty = basisState(session, journal, ReturnKind.Sell, false, false, PageOutcome.page(false)) {}

        assertEquals(ScreenState.Trouble(journal.basisUnread, journal.basisUnreadHint), withoutRetry(unread))
        assertNotNull((unread as ScreenState.Trouble).onRetry, "повторить чтение должно быть чем")
        assertEquals(ScreenState.Empty(iconOf(empty), journal.noSaleBasis, journal.noBasisHint), empty)
    }

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
            PastShiftsView(KassaScene.session("shifts-none", pastShifts = emptyList()))
        }
        val unread = KassaScene.shot("shifts-unread") {
            PastShiftsView(KassaScene.session("shifts-unread"))
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
     * Смена открывается нажатием на её строку, и документы узел отдаёт
     * отдельным обращением: молчание этого обращения выглядело как смена,
     * в которой не пробито ни чека.
     */
    @Test
    fun `открытая смена рисует непрочитанные документы иначе, чем смену без чеков`() {
        val shift = Shift(id = "s-1", shiftNo = 1, status = "CLOSED", closeDocumentId = "d-close")
        val empty = openedShift(KassaScene.session("shift-docs-none", pastShifts = listOf(shift)))
        val unread = openedShift(
            KassaScene.session("shift-docs-unread", pastShifts = listOf(shift), journalAnswered = false)
        )

        assertTrue(empty.isNotEmpty() && unread.isNotEmpty(), "экран не собрался")
        assertTrue(
            !empty.contentEquals(unread),
            "«документов нет» и «прочитать не удалось» на экране смены неразличимы"
        )
    }

    /** Кадр экрана смены, открытой нажатием на первую строку списка. */
    private fun openedShift(session: Session): ByteArray =
        RenderProbe(width = KassaScene.WIDE, height = KassaScene.TALL) { PastShiftsView(session) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(FIRST_ROW)
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }

    /** Кнопка повтора в сравнении не участвует: сравниваются слова, а не замыкания. */
    private fun withoutRetry(state: ScreenState): ScreenState =
        (state as ScreenState.Trouble).copy(onRetry = null)

    /** Значок пустого состояния берётся у него же: проверяются слова, а не картинка. */
    private fun iconOf(state: ScreenState) = (state as ScreenState.Empty).icon

    private companion object {
        const val WHAT = "Чеки дня"

        /** Первая строка списка смен: по ней открывают смену. */
        val FIRST_ROW = Offset(400f, 60f)

        /** Сколько кадров даётся чтению, чтобы доехать до экрана. */
        const val SETTLE = 40
    }
}
