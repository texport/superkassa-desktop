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
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Хозяйство сети в кабинете окна: списки целиком и правка одной строки.
 *
 * Кабинет окна держит точки и кассы компании — по ним ищут в колонке и выбирают
 * точку в заявлении. Проверяется то, из-за чего владелец сети не находил
 * свою кассу: список читался первой страницей. И обратное: карточка кассы
 * опрашивается каждые несколько секунд, пока ИСНА рассматривает заявление,
 * и перечитывать ради неё сорок страниц списка нельзя.
 */
class CabinetListsTest {

    /** Куда сходил кабинет: путь с номером страницы, в порядке обращений. */
    private val calls: MutableList<String> = Collections.synchronizedList(mutableListOf())

    private fun listsWith(total: Int, onPage: (Int) -> Unit = {}): CabinetListsRig {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val page = request.url.parameters["page"]?.toInt() ?: 0
            calls += "$path?page=$page"
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
    fun `кабинет окна читает все точки и все кассы сети`() {
        val rig = listsWith(BIG)
        runBlocking {
            rig.lists.readPlaces()
            rig.lists.readRegisters()
        }
        assertEquals(BIG, rig.state.places.size, "точек в кабинете окна ${rig.state.places.size} из $BIG")
        assertEquals(BIG, rig.state.registers.size, "касс в кабинете окна ${rig.state.registers.size} из $BIG")
    }

    /**
     * Перечитанная карточка меняет свою строку, а не весь список.
     *
     * До правки карточка кассы звала перечитывание списка компании —
     * и каждый круг опроса ИСНА стоил бы сети сорока запросов.
     */
    @Test
    fun `перечитанная касса меняет свою строку без обращения к кабинету`() {
        val rig = listsWith(SMALL)
        runBlocking { rig.lists.readRegisters() }
        calls.clear()

        val changed = rig.state.registers[1].copy(status = "REGISTERED", registrationNumber = "260940000031")
        rig.lists.registerChanged(changed)

        assertEquals(emptyList(), calls, "за одной кассой сходили в кабинет: $calls")
        assertEquals("REGISTERED", rig.state.registers[1].status)
        assertEquals("260940000031", rig.state.registers[1].registrationNumber)
        assertEquals(SMALL, rig.state.registers.size, "список касс потерял строки")
        assertEquals("DRAFT", rig.state.registers[0].status, "соседняя касса изменилась вместе с правленой")
    }

    /**
     * Колонка наполняется страницами, а не ждёт последней.
     *
     * Сорок страниц сети — это секунды, и всё это время колонка стояла бы
     * в ожидании, хотя первые пятьдесят точек уже пришли. Считает ли
     * кабинет, что точек больше прочитанного, видно по объявленному числу:
     * по нему колонка и говорит «Показано 50 из 2000».
     */
    @Test
    fun `точки выкладываются по страницам, а не в конце чтения`() {
        var shownAtSecondPage = -1
        var totalAtSecondPage = -1
        lateinit var rig: CabinetListsRig
        rig = listsWith(BIG) { page ->
            if (page == 1) {
                shownAtSecondPage = rig.state.places.size
                totalAtSecondPage = rig.state.placesTotal
            }
        }
        runBlocking { rig.lists.readPlaces() }

        assertEquals(PAGE, shownAtSecondPage, "к запросу второй страницы колонка ещё пуста")
        assertEquals(BIG, totalAtSecondPage, "сколько точек всего, колонка узнаёт только в конце чтения")
        assertEquals(BIG, rig.state.places.size)
        assertEquals(BIG, rig.state.placesTotal)
        val pages = (0 until BIG / PAGE).map { "/api/retail-places?page=$it" }
        assertEquals(pages, calls.toList(), "страницы спрошены не одним чтением проверки")
    }

    private companion object {
        const val PAGE = 50
        const val BIG = 2000
        const val SMALL = 4
    }
}
