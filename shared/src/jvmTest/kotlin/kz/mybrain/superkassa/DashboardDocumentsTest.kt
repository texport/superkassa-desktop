package kz.mybrain.superkassa

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.data.node.Kkm
import kz.mybrain.superkassa.data.node.ServerClient
import kz.mybrain.superkassa.domain.shift.ShiftState
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.presentation.dashboard.DashboardContent
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.session.refreshSelected
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Число документов смены на главном экране — только то, что назвал узел.
 *
 * У кассы, снятой с учёта, узел держит смену открытой, а на её документы
 * отвечает KKM_BLOCKED. Плитка «Документов за смену» показывала при этом
 * ноль, а на месте списка стояло «Документов пока нет» с обещанием, что
 * первый чек вот-вот появится: кассир читал это как пустую смену и решал
 * по ней, можно ли снимать Z-отчёт.
 */
class DashboardDocumentsTest {

    private val blockedKkm = Kkm(
        kkmId = "b53e0e1c-82d2-4aab-a81c-4a119b3c0fec",
        kkmKgdId = "260940000020",
        state = "BLOCKED",
        blockReasonCode = 1015
    )

    private val openShiftBody = """[{"id":"s6","kkmId":"b53e0e1c","shiftNo":6,"status":"OPEN",""" +
        """"openedAt":1789751716566,"openDocumentId":"d1"}]"""

    private val blockedBody = """{"code":"KKM_BLOCKED","message":"RU: ККМ заблокирована | KK: — | EN: blocked"}"""

    private fun sessionOn(engine: MockEngine): Session {
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-documents").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm"))).also {
            it.select(blockedKkm, remember = false)
            it.adoptPin("4827")
        }
    }

    @Test
    fun `непрочитанные документы открытой смены не считаются нулём`() = runBlocking {
        val session = sessionOn(
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("/shifts")) {
                    respond(openShiftBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                } else {
                    respond(blockedBody, HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        )

        session.refreshSelected()

        assertEquals(ShiftState.Open, session.shiftState)
        assertTrue(session.documents.isEmpty())
        assertFalse(session.documentsRead, "непрочитанный список документов объявлен прочитанным")
    }

    @Test
    fun `прочитанный пустой список остаётся прочитанным`() = runBlocking {
        val documents = """[]"""
        val session = sessionOn(
            MockEngine { request ->
                val body = if (request.url.encodedPath.endsWith("/shifts")) openShiftBody else documents
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

        session.refreshSelected()

        assertEquals(ShiftState.Open, session.shiftState)
        assertTrue(session.documentsRead, "пустой ответ узла объявлен непрочитанным")
    }

    /**
     * Смена без единого чека и смена, документов которой не видно, —
     * разные картинки, а не одна на оба случая.
     */
    @Test
    fun `пустая смена и непрочитанные документы выглядят по-разному`() {
        val empty = DashboardScene.state(shift = CoreScene.openShift())
        val unread = with(DashboardScene) { state(shift = CoreScene.openShift()).unread() }

        val emptyFrame = KassaScene.shot("dash-shift-empty") { DashboardContent(empty) }
        val unreadFrame = KassaScene.shot("dash-shift-unread") { DashboardContent(unread) }

        assertFalse(
            emptyFrame.contentEquals(unreadFrame),
            "смена без чеков и смена, документы которой узел не отдал, показаны одинаково"
        )
    }
}
