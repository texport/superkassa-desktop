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
 * Хозяйство сети в сеансе кабинета: списки целиком и правка одной строки.
 *
 * Сеанс держит точки и кассы компании — по ним ищут в колонке и выбирают
 * точку в заявлении. Проверяется то, из-за чего владелец сети не находил
 * свою кассу: список читался первой страницей. И обратное: карточка кассы
 * опрашивается каждые несколько секунд, пока ИСНА рассматривает заявление,
 * и перечитывать ради неё сорок страниц списка нельзя.
 */
class CabinetSessionListsTest {

    /** Куда сходил сеанс: путь с номером страницы, в порядке обращений. */
    private val calls = mutableListOf<String>()

    private fun sessionWith(total: Int, onPage: (Int) -> Unit = {}): CabinetSession {
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
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return mockCabinet(CabinetClient(http = http))
    }

    private fun row(at: Int) =
        """{"id":"id-$at","kkmId":${at + 1},"status":"DRAFT","name":"Торговая точка $at"}"""

    @Test
    fun `сеанс читает все точки и все кассы сети`() {
        val cabinet = sessionWith(BIG)
        runBlocking {
            cabinet.refreshPlaces()
            cabinet.refreshRegisters()
        }
        assertEquals(BIG, cabinet.places.size, "точек в сеансе ${cabinet.places.size} из $BIG")
        assertEquals(BIG, cabinet.registers.size, "касс в сеансе ${cabinet.registers.size} из $BIG")
    }

    /**
     * Перечитанная карточка меняет свою строку, а не весь список.
     *
     * До правки карточка кассы звала перечитывание списка компании —
     * и каждый круг опроса ИСНА стоил бы сети сорока запросов.
     */
    @Test
    fun `перечитанная касса меняет свою строку без обращения к кабинету`() {
        val cabinet = sessionWith(SMALL)
        runBlocking { cabinet.refreshRegisters() }
        calls.clear()

        val changed = cabinet.registers[1].copy(status = "REGISTERED", registrationNumber = "260940000031")
        cabinet.registerChanged(changed)

        assertEquals(emptyList(), calls, "за одной кассой сходили в кабинет: $calls")
        assertEquals("REGISTERED", cabinet.registers[1].status)
        assertEquals("260940000031", cabinet.registers[1].registrationNumber)
        assertEquals(SMALL, cabinet.registers.size, "список касс потерял строки")
        assertEquals("DRAFT", cabinet.registers[0].status, "соседняя касса изменилась вместе с правленой")
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
        lateinit var cabinet: CabinetSession
        cabinet = sessionWith(BIG) { page ->
            if (page == 1) {
                shownAtSecondPage = cabinet.places.size
                totalAtSecondPage = cabinet.placesTotal
            }
        }
        runBlocking { cabinet.refreshPlaces() }

        assertEquals(PAGE, shownAtSecondPage, "к запросу второй страницы колонка ещё пуста")
        assertEquals(BIG, totalAtSecondPage, "сколько точек всего, колонка узнаёт только в конце чтения")
        assertEquals(BIG, cabinet.places.size)
        assertEquals(BIG, cabinet.placesTotal)
    }

    private companion object {
        const val PAGE = 50
        const val BIG = 2000
        const val SMALL = 4
    }
}
