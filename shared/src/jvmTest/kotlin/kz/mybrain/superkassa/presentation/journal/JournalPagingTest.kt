package kz.mybrain.superkassa.presentation.journal

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.journal.model.DocumentPages
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.common.document.shownNote
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.journal.documents.journalModel
import kz.mybrain.superkassa.presentation.journal.shifts.shiftsModel
import kz.mybrain.superkassa.presentation.journal.shifts.shiftsState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Дочитывание страницами: что сказано о прочитанном и о непрочитанном.
 *
 * Касса отдаёт журнал и смены страницами. Оборванный на странице список —
 * обычное дело у оживлённой кассы, и всё, что о нём сказано, владелец
 * принимает за факт: «показано 200 / 200» он читает как весь день,
 * а «показаны все смены» — как всю историю кассы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalPagingTest {

    private val texts = textsOf(Language.Ru).journal
    private val signIn = SignIn().apply { enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN) }

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `шапка журнала не выдаёт прочитанное за весь срок`() {
        val journal = texts.history

        val whole = shownNote(journal, shown = 12, rows = 60, more = false)
        val part = shownNote(journal, shown = 200, rows = 200, more = true)

        assertEquals("${journal.shown}: 12 / 60", whole)
        assertTrue(
            !part.startsWith("${journal.shown}:"),
            "«${journal.shown}: 200 / 200» над кнопкой «${journal.showMore}» читается как весь срок"
        )
        assertEquals("${journal.shownOfRead}: 200 / 200", part)
    }

    /** Больше пятисот записей за раз касса не отдаёт и отвечает отказом: страница обязана быть меньше. */
    @Test
    fun `страница журнала не больше того, что отдаёт касса`() {
        val limits = mutableListOf<Int>()
        val core = FakeCore().apply {
            on("getDocumentTypes") { emptyList<Any>() }
            on("listFiscalDocumentsByPeriod") { args -> emptyList<Any>().also { limits += args[3] as Int } }
            on("listShifts") { args -> emptyList<Any>().also { limits += args[1] as Int } }
            on("listShiftDocuments") { args -> emptyList<Any>().also { limits += args[2] as Int } }
        }
        journalModel(CoreScene.app(core, signIn))
        shiftsModel(CoreScene.app(core, signIn)).open(shift(1))

        assertTrue(limits.size == 3 && limits.all { it in 1..DocumentPages.CORE_LIMIT }, "страницы: $limits")
    }

    @Test
    fun `неудача дочитывания не объявляет оборванный срок показанным целиком`() {
        val model = journalModel(CoreScene.app(core(answers = 1), signIn))
        val first = model.state.value.page

        model.more()

        val second = model.state.value.page
        assertEquals(PageOutcome.page(more = true), first)
        assertTrue(!second.read, "касса промолчала, и прочитанным срок назвать нечем")
        assertTrue(second.more, "«показан весь срок» под сроком, оборванным на странице, — неправда")
        val documents = model.state.value.documents
        assertEquals(DocumentPages.DOCUMENTS, documents.size, "непришедшая страница строк не добавляет")
    }

    @Test
    fun `неудача дочитывания смен не объявляет историю кассы показанной целиком`() {
        val model = shiftsModel(CoreScene.app(core(answers = 1), signIn))
        val first = model.state.value.page

        model.more()

        assertEquals(PageOutcome.page(more = true), first)
        assertTrue(model.state.value.page.more, "«показаны все смены» после молчания кассы — неправда")
        assertEquals(DocumentPages.SHIFTS, model.state.value.shifts.size)
    }

    /**
     * Чек, пробитый между двумя обращениями, сдвигает счёт страниц кассы,
     * и в следующей странице приходит уже показанный документ. Строка
     * журнала различается его же ключом, и повтор ронял список целиком.
     */
    @Test
    fun `дочитывание не удваивает строку, пришедшую дважды`() {
        val model = journalModel(CoreScene.app(core(answers = 2, growing = true), signIn))

        model.more()

        val documents = model.state.value.documents
        assertEquals(documents.size, documents.distinctBy { it.id }.size, "один и тот же чек стоит в журнале дважды")
        assertEquals(DocumentPages.DOCUMENTS * 2 - 1, documents.size, "строки следующей страницы потерялись")
    }

    /** Прочитанные смены остаются на экране, пока читается следующая страница. */
    @Test
    fun `дочитывание смен не убирает с экрана уже прочитанные`() {
        val journal = texts.shifts

        val more = shiftsState(journal, shifts = DocumentPages.SHIFTS, loading = true, page = PageOutcome.page(true)) {}
        val first = shiftsState(journal, shifts = 0, loading = true, page = PageOutcome.unread) {}

        assertEquals(ScreenState.Ready, more, "список смен пропадал на время дочитывания")
        assertEquals(ScreenState.Working, first, "до первого ответа показывать нечего")
    }

    /** Касса, которая отвечает полной страницей ровно [answers] раз, а дальше отказывает. */
    private fun core(answers: Int, growing: Boolean = false): FakeCore {
        var answered = 0
        return FakeCore().apply {
            on("getDocumentTypes") { emptyList<Any>() }
            on("listFiscalDocumentsByPeriod") { args ->
                check(answered < answers) { "kassa is silent" }
                // У растущего списка каждый новый чек встаёт в начало
                // и сдвигает счёт страниц на одну строку назад.
                val offset = args[4] as Int
                val from = (if (growing) offset - answered else offset) + 1L
                answered += 1
                (from until from + DocumentPages.DOCUMENTS).map { CoreScene.document("d-$it") }
            }
            on("listShifts") {
                check(answered < answers) { "kassa is silent" }
                answered += 1
                (1L..DocumentPages.SHIFTS).map(::shift)
            }
        }
    }

    private fun shift(no: Long) =
        ShiftResponse(id = "s-$no", kkmId = "kkm-1", shiftNo = no, status = ShiftStatus.CLOSED, openedAt = 0)
}
