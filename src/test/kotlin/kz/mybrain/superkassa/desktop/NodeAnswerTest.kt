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
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.server.ServerClient
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Отказ узла и молчание узла — разные вещи, и обе видны на входе.
 *
 * Связь считалась только по удачным обращениям: у узла, который поднялся
 * и отвечает отказом, шапка писала «Узел недоступен», и кассир звал
 * обслуживание к работающему узлу. Отказ по существу — это ответ.
 *
 * При этом отказ ничего не говорит о кассах на узле: список остаётся
 * непрочитанным, и объявлять «на этом узле ни одной кассы» по нему нельзя.
 */
class NodeAnswerTest {

    private val refusal = """{"code":"KKM_LIST_FORBIDDEN","message":"RU: Нет доступа | KK: — | EN: forbidden"}"""

    private fun sessionOn(engine: MockEngine): Session {
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-answer").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    @Test
    fun `узел, ответивший отказом, считается на связи`() = runBlocking {
        val session = sessionOn(
            MockEngine {
                respond(refusal, HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

        session.refreshKkms()

        assertTrue(session.nodeAvailable, "узел ответил отказом, а показан недоступным")
        assertFalse(session.kkmsRead, "отказ узла принят за прочитанный список касс")
    }

    @Test
    fun `узел, который не отвечает, на связи не считается`() = runBlocking {
        val session = sessionOn(MockEngine { throw IOException("узел не отвечает") })

        session.refreshKkms()

        assertFalse(session.nodeAvailable)
        assertFalse(session.kkmsRead)
    }

    @Test
    fun `прочитанный пустой список остаётся прочитанным`() = runBlocking {
        val session = sessionOn(
            MockEngine {
                respond(EMPTY_PAGE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

        session.refreshKkms()

        assertTrue(session.nodeAvailable)
        assertTrue(session.kkmsRead, "узел ответил пустым списком, а список показан непрочитанным")
        assertTrue(session.kkms.isEmpty())
    }

    private companion object {
        /** Пустая страница узла: перечни он отдаёт обёрнутыми, а не голым массивом. */
        const val EMPTY_PAGE = """{"items":[],"total":0,"limit":200,"offset":0,"hasMore":false}"""
    }
}
