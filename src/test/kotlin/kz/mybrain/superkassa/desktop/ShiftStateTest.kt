package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Состояние смены на экране совпадает с состоянием смены на узле.
 *
 * Раньше открытость смены выводилась из того, ответил ли узел на список
 * документов текущей смены. У кассы 260940000020, снятой с учёта, он
 * отвечает KKM_BLOCKED: экран писал «Смена закрыта» и предлагал
 * «Открыть смену» над сменой 6, которую узел держал открытой с 18 сентября,
 * а нажатие получало SHIFT_ALREADY_OPEN.
 */
class ShiftStateTest {

    private fun sessionWith(engine: MockEngine): Session {
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-shift").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    private fun sessionOn(kkm: Kkm, engine: MockEngine): Session {
        val session = sessionWith(engine)
        session.select(kkm, remember = false)
        session.adoptPin("4827")
        return session
    }

    private val blockedKkm = Kkm(
        kkmId = "b53e0e1c-82d2-4aab-a81c-4a119b3c0fec",
        kkmKgdId = "260940000020",
        state = "BLOCKED",
        blockReasonCode = 1015
    )

    private val openShiftBody = """[{"id":"s6","kkmId":"b53e0e1c","shiftNo":6,"status":"OPEN",""" +
        """"openedAt":1789751716566,"openDocumentId":"d1"}]"""

    private val blockedBody = """{"code":"KKM_BLOCKED","message":"RU: ККМ заблокирована | KK: — | EN: blocked"}"""

    @Test
    fun `открытая на узле смена показывается открытой, хотя документы узел не отдаёт`() = runBlocking {
        val session = sessionOn(
            blockedKkm,
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("/shifts")) {
                    respond(openShiftBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                } else {
                    respond(
                        blockedBody,
                        HttpStatusCode.BadRequest,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        )

        session.refreshSelected()

        assertEquals(ShiftState.Open, session.shiftState)
        assertTrue(session.shiftOpen)
        assertEquals(6L, session.shiftNumber)
    }

    @Test
    fun `закрытая на узле смена показывается закрытой`() = runBlocking {
        val closed = """[{"id":"s5","kkmId":"b53e0e1c","shiftNo":5,"status":"CLOSED",""" +
            """"openedAt":1,"closedAt":2,"closeDocumentId":"z5"}]"""
        val session = sessionOn(
            blockedKkm,
            MockEngine {
                respond(closed, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

        session.refreshSelected()

        assertEquals(ShiftState.Closed, session.shiftState)
        assertFalse(session.shiftOpen)
    }

    /**
     * Узел о смене не ответил — приложение не называет состояние за него.
     *
     * «Смена закрыта» на месте неизвестности толкает кассира открыть смену,
     * которую узел, возможно, уже держит открытой.
     */
    @Test
    fun `состояние смены, о котором узел молчит, остаётся неизвестным`() = runBlocking {
        val session = sessionOn(blockedKkm, MockEngine { throw java.io.IOException("узел не отвечает") })

        session.refreshSelected()

        assertEquals(ShiftState.Unknown, session.shiftState)
        assertFalse(session.shiftOpen)
    }
}
