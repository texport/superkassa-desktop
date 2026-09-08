package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.ServerClient
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Поведение сеанса при отказе узла и при его недоступности.
 *
 * Разделять эти два случая обязательно: при отказе кассир правит чек,
 * при недоступности зовёт обслуживание, и одинаковое сообщение увело бы
 * его не туда.
 */
class SessionTest {

    /**
     * Сеанс с временным хранилищем настроек.
     *
     * Настройки обязательно свои: сеанс с настройками по умолчанию пишет
     * в `~/.superkassa/kkm` — тот самый файл, из которого рабочая касса
     * узнаёт кассу этого места. Тест выбирал кассу «b2», и после каждой
     * сборки кассир на этой машине получал «касса не выбрана».
     */
    private fun sessionWith(engine: MockEngine): Session {
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-session").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    @Test
    fun `список касс читается и узел считается доступным`() = runBlocking {
        // Узел заворачивает перечни: массив, общее число и признак продолжения.
        // Прежний вид ответа в этом тесте был выдуман и прятал настоящую ошибку —
        // приложение не разбирало список касс вовсе.
        val body = """{"items":[{"kkmId":"a1","ofdSystemId":"2000001","state":"IDLE"}],""" +
            """"total":1,"limit":500,"offset":0,"hasMore":false}"""
        val session = sessionWith(
            MockEngine {
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

        session.refreshKkms()

        assertEquals(1, session.kkms.size)
        assertEquals("a1", session.kkms.first().kkmId)
        assertTrue(session.nodeAvailable)
    }

    @Test
    fun `отказ узла показывается его словами и не гасит связь`() = runBlocking {
        val body = """{"code":"KKM_BLOCKED","message":"RU: Касса заблокирована | KK: — | EN: Blocked"}"""
        val session = sessionWith(
            MockEngine {
                respond(body, HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

        val result = session.guard<Unit>("Проверка") { session.client.listKkmsForTest() }

        assertNull(result)
        val message = session.lastMessage
        assertTrue(message is Message.Refusal)
        assertEquals("Касса заблокирована", message.text)
        assertEquals("KKM_BLOCKED", message.code)
    }

    @Test
    fun `недоступность узла отмечается отдельно от отказа`() = runBlocking {
        val session = sessionWith(MockEngine { throw java.io.IOException("узел не отвечает") })

        session.refreshKkms()

        assertTrue(session.lastMessage is Message.NodeUnavailable)
        assertTrue(!session.nodeAvailable)
    }

    @Test
    fun `выбор кассы сбрасывает данные прежней`() {
        val session = sessionWith(MockEngine { respondError(HttpStatusCode.NotFound) })
        session.documents.add(
            kz.mybrain.superkassa.desktop.server.Document(id = "d1")
        )

        session.select(kz.mybrain.superkassa.desktop.server.Kkm(kkmId = "b2"))

        assertTrue(session.documents.isEmpty())
        assertEquals("b2", session.selected?.kkmId)
    }
}

private suspend fun ServerClient.listKkmsForTest() {
    val response = call(io.ktor.http.HttpMethod.Get, "/kkm", null, null)
    if (response.status.value !in 200..299) throw refusalOf(response)
}
