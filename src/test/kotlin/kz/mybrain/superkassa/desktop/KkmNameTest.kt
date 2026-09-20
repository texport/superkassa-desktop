package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.adoptCabinetNames
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.rename
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Как кассу зовут на экране и где это название живёт.
 *
 * Названия хранит узел: владелец даёт его один раз, и видит его любое
 * рабочее место, ещё до того как кассир набрал пин. Своё название
 * рабочего места остаётся запасным — на случай, когда узел его не принял.
 */
class KkmNameTest {

    /** Касса без своего названия зовётся так, как её назвали на узле. */
    @Test
    fun `название с узла показывается вместо регистрационного номера`() = runBlocking {
        val session = sessionThatAnswers(listOf(node(name = "Касса 2 на Достык")))
        session.refreshKkms()

        assertEquals("Касса 2 на Достык", session.displayName(session.kkms.first()))
    }

    /** Названного руками здесь узел не перебивает: его дали позже и зная про узловое. */
    @Test
    fun `своё название рабочего места сильнее названия с узла`() = runBlocking {
        val session = sessionThatAnswers(listOf(node(name = "Касса 2 на Достык")), nameAccepted = false)
        session.refreshKkms()
        session.adoptPin(PIN)
        session.rename(session.kkms.first(), "Вторая линия")

        assertEquals("Вторая линия", session.displayName(session.kkms.first()))
    }

    /** Названия нет нигде — остаётся то, что написано на самой машине. */
    @Test
    fun `без названий касса зовётся регистрационным номером`() = runBlocking {
        val session = sessionThatAnswers(listOf(node(name = null)))
        session.refreshKkms()

        assertEquals(REGISTRATION_NUMBER, session.displayName(session.kkms.first()))
    }

    /**
     * Один вход владельца в кабинет называет кассу для всех рабочих мест:
     * название уходит на узел, а не оседает на этой машине.
     */
    @Test
    fun `название из кабинета уходит на узел`() = runBlocking {
        val written = mutableListOf<String>()
        val session = sessionThatAnswers(listOf(node(name = null)), written = written)
        session.refreshKkms()
        session.adoptPin(PIN)

        session.adoptCabinetNames(listOf(cabinet("Касса 2 на Достык")))

        assertEquals(listOf("""{"name":"Касса 2 на Достык"}"""), written)
        assertEquals("Касса 2 на Достык", session.displayName(session.kkms.first()))
    }

    /** У кассы название на узле уже есть: кабинет его не переписывает. */
    @Test
    fun `названную на узле кассу кабинет не трогает`() = runBlocking {
        val written = mutableListOf<String>()
        val session = sessionThatAnswers(listOf(node(name = "Касса у входа")), written = written)
        session.refreshKkms()
        session.adoptPin(PIN)

        session.adoptCabinetNames(listOf(cabinet("Касса 2 на Достык")))

        assertTrue(written.isEmpty(), written.toString())
        assertEquals("Касса у входа", session.displayName(session.kkms.first()))
    }

    /** Узел не принял название — касса всё равно зовётся им на этой машине. */
    @Test
    fun `отказ узла оставляет название на рабочем месте`() = runBlocking {
        val session = sessionThatAnswers(listOf(node(name = null)), nameAccepted = false)
        session.refreshKkms()
        session.adoptPin(PIN)

        session.adoptCabinetNames(listOf(cabinet("Касса 2 на Достык")))

        assertEquals("Касса 2 на Достык", session.displayName(session.kkms.first()))
    }

    /**
     * Узел, который отдаёт список касс и принимает название.
     *
     * Принятое название попадает в список: рабочее место перечитывает его
     * после записи, и проверять надо то, что оно оттуда прочтёт.
     */
    private fun sessionThatAnswers(
        kkms: List<String>,
        nameAccepted: Boolean = true,
        written: MutableList<String> = mutableListOf()
    ): Session {
        var current = kkms
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Put && request.url.encodedPath.endsWith(NAME_PATH) -> {
                    val body = (request.body as TextContent).text
                    if (!nameAccepted) {
                        refusal()
                    } else {
                        written.add(body)
                        val chosen = body.substringAfter("\"name\":\"").substringBefore("\"")
                        current = listOf(node(name = chosen))
                        respondJson(current.first())
                    }
                }

                else -> respondJson("""{"items":[${current.joinToString(",")}],"total":${current.size}""" +
                    ""","limit":500,"offset":0,"hasMore":false}""")
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-name").toFile()
        directory.deleteOnExit()
        return Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
    }

    private fun MockRequestHandleScope.respondJson(body: String): HttpResponseData =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, JSON))

    private fun MockRequestHandleScope.refusal(): HttpResponseData =
        respond(
            """{"code":"KKM_NOT_FOUND","message":"RU: Касса не найдена | KK: — | EN: Not found"}""",
            HttpStatusCode.NotFound,
            headersOf(HttpHeaders.ContentType, JSON)
        )

    private fun node(name: String?): String {
        val named = name?.let { ""","name":"$it"""" }.orEmpty()
        return """{"kkmId":"$KKM_ID","ofdSystemId":"$SYSTEM_ID",""" +
            """"kkmKgdId":"$REGISTRATION_NUMBER","state":"IDLE"$named}"""
    }

    private fun cabinet(name: String) = CabinetRegister(
        id = "c1",
        kkmId = SYSTEM_ID.toInt(),
        internalName = name,
        status = "REGISTERED",
        registrationNumber = REGISTRATION_NUMBER
    )

    private companion object {
        const val KKM_ID = "a1"
        const val SYSTEM_ID = "5000021"
        const val REGISTRATION_NUMBER = "260940000021"
        const val PIN = "4827"
        const val NAME_PATH = "/settings/name"
        const val JSON = "application/json"
    }
}
