package kz.mybrain.superkassa.presentation.cabinet

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetListsRig
import kz.mybrain.superkassa.CabinetWire
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

    private fun listsWith(total: Int, onPage: (Int) -> Unit = {}): CabinetListsRig {
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
            install(ContentNegotiation) { json(CabinetWire.json) }
        }
        return CabinetListsRig(CabinetWire(http = http))
    }

    private fun row(at: Int) =
        """{"id":"id-$at","kkmId":${at + 1},"status":"DRAFT","name":"Торговая точка $at"}"""

    @Test
    fun `перечитывание точек не опустошает уже показанную колонку`() {
        lateinit var rig: CabinetListsRig
        rig = listsWith(BIG) { shownAtPage += rig.state.places.size }
        runBlocking { rig.lists.readPlaces() }
        shownAtPage.clear()

        runBlocking { rig.lists.readPlaces() }

        assertEquals(BIG, shownAtPage.min(), "во время перечитывания колонка падала до ${shownAtPage.min()} точек")
        assertEquals(BIG, rig.state.places.size)
    }

    @Test
    fun `перечитывание касс не опустошает уже показанный список`() {
        lateinit var rig: CabinetListsRig
        rig = listsWith(BIG) { shownAtPage += rig.state.registers.size }
        runBlocking { rig.lists.readRegisters() }
        shownAtPage.clear()

        runBlocking { rig.lists.readRegisters() }

        assertEquals(BIG, shownAtPage.min(), "во время перечитывания список падал до ${shownAtPage.min()} касс")
    }

    /** Точку удалили в другом окне: перечитанный список стал короче — и таким и остаётся. */
    @Test
    fun `перечитывание принимает укоротившийся список`() {
        val rig = listsWith(BIG)
        runBlocking { rig.lists.readPlaces() }
        assertEquals(BIG, rig.state.places.size)
        val shorter = listsWith(BIG - 1)
        shorter.screen.value = rig.state

        runBlocking { shorter.lists.readPlaces() }

        assertEquals(BIG - 1, shorter.state.places.size)
    }

    private companion object {
        const val PAGE = 50
        const val BIG = 200
    }
}
