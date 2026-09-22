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
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Перечитывание списка на глазах у владельца.
 *
 * Список выкладывается страницами по мере чтения — это верно для первого
 * чтения, когда колонка пуста. При втором чтения того же списка владелец
 * уже смотрит на тысячу точек, и та же выкладка сбрасывала колонку до
 * первой полусотни и набирала её заново: «Показано 50 из 1004» посреди
 * работы, съехавшая прокрутка и пропавшая из списка выбранная точка.
 * Перечитывание идёт после заведения точки, после правки кассы и при
 * каждом открытии карточки кассы — то есть постоянно.
 */
class CabinetListRereadTest {

    /** Сколько записей видно в колонке к началу каждой страницы ответа. */
    private val shownAtPage = mutableListOf<Int>()

    private fun sessionWith(total: Int, onPage: (Int) -> Unit = {}): CabinetSession {
        val engine = MockEngine { request ->
            val page = request.url.parameters["page"]?.toInt() ?: 0
            onPage(page)
            val from = page * PAGE
            val items = (from until minOf(from + PAGE, total)).joinToString(",") { row(it) }
            respond(
                content = """{"page":$page,"size":$PAGE,"totalElements":$total,"items":[$items]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return mockCabinet(CabinetClient(http = http))
    }

    private fun row(at: Int) =
        """{"id":"id-$at","kkmId":${at + 1},"status":"DRAFT","name":"Торговая точка $at"}"""

    @Test
    fun `перечитывание точек не опустошает уже показанную колонку`() {
        lateinit var cabinet: CabinetSession
        cabinet = sessionWith(BIG) { shownAtPage += cabinet.places.size }
        runBlocking { cabinet.refreshPlaces() }
        shownAtPage.clear()

        runBlocking { cabinet.refreshPlaces() }

        assertEquals(BIG, shownAtPage.min(), "во время перечитывания колонка падала до ${shownAtPage.min()} точек")
        assertEquals(BIG, cabinet.places.size)
    }

    @Test
    fun `перечитывание касс не опустошает уже показанный список`() {
        lateinit var cabinet: CabinetSession
        cabinet = sessionWith(BIG) { shownAtPage += cabinet.registers.size }
        runBlocking { cabinet.refreshRegisters() }
        shownAtPage.clear()

        runBlocking { cabinet.refreshRegisters() }

        assertEquals(BIG, shownAtPage.min(), "во время перечитывания список падал до ${shownAtPage.min()} касс")
    }

    /** Точку удалили в другом окне: перечитанный список стал короче — и таким и остаётся. */
    @Test
    fun `перечитывание принимает укоротившийся список`() {
        lateinit var cabinet: CabinetSession
        cabinet = sessionWith(BIG)
        runBlocking { cabinet.refreshPlaces() }
        val shorter = sessionWith(BIG - 1)
        cabinet.places.let { assertEquals(BIG, it.size) }

        runBlocking { shorter.refreshPlaces() }

        assertEquals(BIG - 1, shorter.places.size)
    }

    private companion object {
        const val PAGE = 50
        const val BIG = 200
    }
}
