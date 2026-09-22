package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.PAGE
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import kz.mybrain.superkassa.desktop.ui.history.PageOutcome
import kz.mybrain.superkassa.desktop.ui.history.SHIFT_PAGE
import kz.mybrain.superkassa.desktop.ui.history.Shift
import kz.mybrain.superkassa.desktop.ui.history.loadPeriod
import kz.mybrain.superkassa.desktop.ui.history.loadShifts
import kz.mybrain.superkassa.desktop.ui.history.shiftsState
import kz.mybrain.superkassa.desktop.ui.history.shownNote
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Дочитывание страницами: что сказано о прочитанном и о непрочитанном.
 *
 * Узел отдаёт журнал и смены страницами по двести. Оборванный на двухсотой
 * список — обычное дело у оживлённой кассы, и всё, что о нём сказано,
 * владелец принимает за факт: «показано 200 / 200» он читает как весь
 * день, а «показаны все смены» — как всю историю кассы.
 */
class JournalPagingTest {

    private val texts = journalTexts(Language.Ru)

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

    @Test
    fun `неудача дочитывания не объявляет оборванный срок показанным целиком`() = runBlocking {
        val session = session(answers = 1)
        val into = mutableListOf<Document>()
        val period = JournalPeriod.of(JournalSpan.Day)

        val first = loadPeriod(session, WHAT, period, into)
        val second = loadPeriod(session, WHAT, period, into, first)

        assertEquals(PageOutcome.page(more = true), first)
        assertTrue(!second.read, "узел промолчал, и прочитанным срок назвать нечем")
        assertTrue(second.more, "«показан весь срок» под сроком, оборванным на двухсотой, — неправда")
        assertEquals(PAGE, into.size, "непришедшая страница строк не добавляет")
    }

    @Test
    fun `неудача дочитывания смен не объявляет историю кассы показанной целиком`() = runBlocking {
        val session = session(answers = 1)
        val into = mutableListOf<Shift>()

        val first = loadShifts(session, WHAT, into)
        val second = loadShifts(session, WHAT, into, first)

        assertEquals(PageOutcome.page(more = true), first)
        assertTrue(second.more, "«показаны все смены» после молчания узла — неправда")
        assertEquals(SHIFT_PAGE, into.size)
    }

    /**
     * Чек, пробитый между двумя обращениями, сдвигает счёт страниц узла,
     * и в следующей странице приходит уже показанный документ. Строка
     * журнала различается его же ключом, и повтор ронял список целиком.
     */
    @Test
    fun `дочитывание не удваивает строку, пришедшую дважды`() = runBlocking {
        val session = session(answers = 2, growing = true)
        val into = mutableListOf<Document>()
        val period = JournalPeriod.of(JournalSpan.Day)

        val first = loadPeriod(session, WHAT, period, into)
        loadPeriod(session, WHAT, period, into, first)

        assertEquals(into.size, into.distinctBy { it.id }.size, "один и тот же чек стоит в журнале дважды")
        assertEquals(PAGE * 2 - 1, into.size, "строки следующей страницы потерялись")
    }

    /** Прочитанные смены остаются на экране, пока читается следующая страница. */
    @Test
    fun `дочитывание смен не убирает с экрана уже прочитанные`() {
        val journal = texts.shifts

        val more = shiftsState(journal, shifts = SHIFT_PAGE, loading = true, page = PageOutcome.page(true)) {}
        val first = shiftsState(journal, shifts = 0, loading = true, page = PageOutcome.unread) {}

        assertEquals(ScreenState.Ready, more, "список смен пропадал на время дочитывания")
        assertEquals(ScreenState.Working, first, "до первого ответа показывать нечего")
    }

    /**
     * Рабочее место, узел которого отвечает полной страницей ровно
     * [answers] раз, а дальше молчит.
     */
    private fun session(answers: Int, growing: Boolean = false): Session {
        var answered = 0
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/users/me") -> answer(WHOAMI)
                answered >= answers -> respondError(HttpStatusCode.ServiceUnavailable)
                path.endsWith("/documents") -> {
                    // У растущего списка каждый новый чек встаёт в начало
                    // и сдвигает счёт страниц на одну строку назад.
                    val offset = request.url.parameters["offset"]?.toInt() ?: 0
                    val from = (if (growing) offset - answered else offset) + 1L
                    answered += 1
                    answer(ServerClient.lenientJson.encodeToString(DOCUMENTS_PAGE, documents(from)))
                }

                path.endsWith("/shifts") -> {
                    answered += 1
                    answer(ServerClient.lenientJson.encodeToString(SHIFTS_PAGE, shifts()))
                }

                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("journal-paging").toFile()
        val session = Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
        session.switchLanguage(Language.Ru)
        runBlocking { session.signIn(KassaScene.kkm(), KassaScene.PIN) }
        return session
    }

    private fun documents(from: Long = 1): List<Document> =
        (from until from + PAGE).map { Document(id = "d-$it", docNo = it) }

    private fun shifts(): List<Shift> = (1L..SHIFT_PAGE).map { Shift(id = "s-$it", shiftNo = it) }

    private fun MockRequestHandleScope.answer(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private companion object {
        const val WHAT = "Журнал"
        const val WHOAMI = """{"userId":"u-1","name":"Айгүл Сәрсенова","role":"ADMIN"}"""
        val DOCUMENTS_PAGE = ListSerializer(Document.serializer())
        val SHIFTS_PAGE = ListSerializer(Shift.serializer())
    }
}
