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
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.allKkmModels
import kz.mybrain.superkassa.desktop.server.cabinet.allRegisters
import kz.mybrain.superkassa.desktop.server.cabinet.allRetailPlaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Точки, кассы и модели читаются целиком, а не первой страницей.
 *
 * Кабинет отдаёт список страницами по пятьдесят. По точкам и кассам идёт
 * поиск в колонке и выбор в заявлении, и оборванный список отвечал
 * «ничего не нашлось» о кассе, которая у владельца есть: у сети из двух
 * тысяч точек владелец видел пятьдесят первых, а кассу № 1900 не находил
 * ни по номеру КГД, ни по заводскому.
 *
 * Кабинет здесь подставной: две тысячи записей раздаются страницами, как
 * это делает настоящий, и проверяется, что прочитаны все и что каждая
 * страница спрошена ровно один раз.
 */
class CabinetWholeListTest {

    /** Сколько страниц спросили и с какими номерами. */
    private val asked = mutableListOf<Int>()

    /**
     * Подставной кабинет со списком из [total] записей.
     *
     * @param give сколько записей отдавать на странице; меньше объявленного
     *   размера — так ведёт себя кабинет, обещающий больше, чем у него есть.
     */
    private fun cabinetWith(total: Int, give: Int = PAGE): CabinetClient {
        val engine = MockEngine { request ->
            val page = request.url.parameters["page"]?.toInt() ?: 0
            asked += page
            val from = page * PAGE
            val items = (from until minOf(from + give, total)).joinToString(",") { row(it) }
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
        return CabinetClient(http = http)
    }

    /**
     * Запись, годная и как точка, и как касса, и как модель.
     *
     * Разбор у трёх списков разный, а страницы одни и те же: одна запись
     * со всеми обязательными полями избавляет от трёх почти одинаковых.
     */
    private fun row(at: Int) = """{"id":"id-$at","kkmId":${at + 1},"status":"REGISTERED",
        "name":"Торговая точка $at","modelCode":"M$at","registrationNumber":"${KGD_FIRST + at}"}"""

    @Test
    fun `две тысячи точек читаются все`() {
        val client = cabinetWith(BIG)
        val places = runBlocking { client.allRetailPlaces("token") }
        assertEquals(BIG, places.size, "точек прочитано ${places.size} из $BIG")
        assertEquals("Торговая точка 1999", places.last().name)
        assertEquals((0 until BIG / PAGE).toList(), asked, "страницы спрошены не подряд или дважды")
    }

    @Test
    fun `две тысячи касс читаются все`() {
        val client = cabinetWith(BIG)
        val registers = runBlocking { client.allRegisters("token") }
        assertEquals(BIG, registers.size)
        assertEquals("${KGD_FIRST + BIG - 1}", registers.last().registrationNumber)
    }

    @Test
    fun `справочник моделей читается целиком`() {
        val client = cabinetWith(MODELS)
        val models = runBlocking { client.allKkmModels("token") }
        assertEquals(MODELS, models.size)
    }

    /**
     * Кабинет объявил больше, чем отдаёт, — чтение кончается, а не крутится.
     *
     * Пустая страница раньше объявленного числа означает конец списка:
     * без этого владелец смотрел бы в занятый экран, пока кабинет отдаёт
     * пустоту.
     */
    @Test
    fun `обещанное сверх отданного не зацикливает чтение`() {
        val client = cabinetWith(total = BIG, give = 0)
        val places = runBlocking { client.allRetailPlaces("token") }
        assertTrue(places.isEmpty())
        assertEquals(listOf(0, 1), asked, "после пустой страницы спрошено лишнее: $asked")
    }

    @Test
    fun `короткий список читается одним запросом`() {
        val client = cabinetWith(SMALL)
        val places = runBlocking { client.allRetailPlaces("token") }
        assertEquals(SMALL, places.size)
        assertEquals(listOf(0), asked, "за коротким списком сходили дважды")
    }

    private companion object {
        /** Номер КГД первой кассы подставного кабинета: дальше по порядку. */
        const val KGD_FIRST = 260940000000

        const val PAGE = 50
        const val BIG = 2000
        const val MODELS = 320
        const val SMALL = 7
    }
}
