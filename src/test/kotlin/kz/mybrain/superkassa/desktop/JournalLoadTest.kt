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
import kz.mybrain.superkassa.desktop.ui.history.JournalLoad
import kz.mybrain.superkassa.desktop.ui.history.loadDay
import kz.mybrain.superkassa.desktop.ui.strings.Language
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Чтение документов дня: пустой день и неудавшееся чтение — разные исходы.
 *
 * Прежде чтение отвечало одним «продолжать ли», и отказ узла доходил
 * до кассира надписью «подходящих чеков нет». Покупатель при этом стоял
 * у кассы с чеком в руках, и кассир искал беду с чеком, которой нет.
 */
class JournalLoadTest {

    @Test
    fun `пустой день прочитан, а молчание узла чтением не считается`() = runBlocking {
        val read = loadDay(session(answering = true), "документы", DAY, mutableListOf())
        val failed = loadDay(session(answering = false), "документы", DAY, mutableListOf())

        assertEquals(JournalLoad.Whole, read, "узел ответил, и за этот день документов нет")
        assertEquals(JournalLoad.Failed, failed, "узел не ответил: это не пустой день")
        assertTrue(failed.failed && !failed.more, "после отказа дочитывать нечего")
    }

    @Test
    fun `касса не выбрана — читать не у кого`() = runBlocking {
        val session = session(answering = true, signedIn = false)

        assertEquals(JournalLoad.Failed, loadDay(session, "документы", DAY, mutableListOf()))
    }

    @Test
    fun `полная страница обещает продолжение, неполная — конец`() {
        val full = mutableListOf<Document>()
        val session = session(answering = true, documents = List(PAGE) { Document(id = "d-$it") })

        val load = runBlocking { loadDay(session, "документы", DAY, full) }

        assertEquals(JournalLoad.More, load)
        assertEquals(PAGE, full.size)
    }

    private fun session(
        answering: Boolean,
        signedIn: Boolean = true,
        documents: List<Document> = emptyList()
    ): Session {
        val body = ServerClient.lenientJson.encodeToString(ListSerializer(Document.serializer()), documents)
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/users/me") -> answer(ME)
                answering -> answer(body)
                else -> respondError(HttpStatusCode.ServiceUnavailable)
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("journal-load").toFile()
        val session = Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
        session.switchLanguage(Language.Ru)
        if (signedIn) runBlocking { session.signIn(KassaScene.kkm(), KassaScene.PIN) }
        return session
    }

    private fun MockRequestHandleScope.answer(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private companion object {
        val DAY: LocalDate = LocalDate.now()
        const val ME = """{"userId":"u-1","name":"Айгүл Сәрсенова","role":"CASHIER"}"""
    }
}
