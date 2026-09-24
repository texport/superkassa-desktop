package kz.mybrain.superkassa.data.cabinet.documents

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.domain.cabinet.model.OKED_PAGE
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentPeriod
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReceiptSearch
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.signedPorts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Что кабинет получает от рабочего места: срок документов и виды
 * деятельности.
 *
 * Здесь — как срок уходит в запрос: параметрами или телом, одной границей
 * или двумя. Как срок считается от окна журнала — `CabinetPeriodOfTest`
 * экранов.
 *
 * Здесь же признак основного вида деятельности: кабинет ждёт примитив
 * `boolean` и на пропущенном поле отвечает отказом разбора — а пропускался
 * он ровно у неосновных, то есть у всех, кроме первого.
 */
class CabinetPeriodTest {

    @Test
    fun `пустой срок не добавляет параметров`() {
        assertEquals("page=0&size=50", reportsAsked(DocumentPeriod()), "пустой срок добавил параметры в запрос")
    }

    @Test
    fun `границы уходят параметрами запроса`() {
        val from = "2026-09-01T00:00:00Z"
        val to = "2026-09-08T00:00:00Z"
        assertEquals(
            "page=0&size=50&dateFrom=2026-09-01T00%3A00%3A00Z&dateTo=2026-09-08T00%3A00%3A00Z",
            reportsAsked(DocumentPeriod(from, to))
        )
    }

    @Test
    fun `одна граница не тянет за собой вторую`() {
        val from = reportsAsked(DocumentPeriod("2026-09-01T00:00:00Z"))
        val to = reportsAsked(DocumentPeriod(to = "2026-09-08T00:00:00Z"))
        assertEquals("page=0&size=50&dateFrom=2026-09-01T00%3A00%3A00Z", from)
        assertEquals("page=0&size=50&dateTo=2026-09-08T00%3A00%3A00Z", to)
    }

    @Test
    fun `у смен срока нет — кабинет их по дате не отбирает`() {
        assertTrue(DocumentKind.Receipts.dated)
        assertTrue(DocumentKind.Reports.dated)
        assertTrue(DocumentKind.CashMovements.dated)
        assertTrue(!DocumentKind.Shifts.dated, "ряд сегментов обещал бы отбор, которого не будет")
    }

    @Test
    fun `границы срока уходят в теле отбора чеков`() {
        val sent = mutableListOf<HttpRequestData>()
        val client = capturing(sent, EMPTY_PAGE)
        val period = DocumentPeriod("2026-09-01T00:00:00Z")
        runBlocking {
            client.documents.receipts("id", ReceiptSearch(dateFrom = period.from, dateTo = period.to))
        }
        val body = (sent.single().body as TextContent).text
        assertTrue(body.contains("\"dateFrom\":\"2026-09-01T00:00:00Z\""), body)
    }

    @Test
    fun `границы срока уходят параметрами у отчётов и движения денег`() {
        val sent = mutableListOf<HttpRequestData>()
        val client = capturing(sent, EMPTY_PAGE)
        val period = DocumentPeriod("2026-09-01T00:00:00Z")
        runBlocking {
            client.documents.reports("id", page = 0, period = period)
            client.documents.movements("id", page = 0, period = period)
        }
        sent.forEach { assertTrue(it.url.toString().contains("dateFrom=2026-09-01T00%3A00%3A00Z"), it.url.toString()) }
    }

    @Test
    fun `неосновной вид деятельности уходит с явным признаком`() {
        val sent = mutableListOf<HttpRequestData>()
        val client = capturing(sent, """{"companyId":"c","okeds":[]}""")
        runBlocking {
            client.company.saveOkeds(
                listOf(Oked("47111", "Розничная торговля", primary = true), Oked("47112", "Ещё один"))
            )
        }
        val body = (sent.single().body as TextContent).text
        assertTrue(body.contains("\"primary\":false"), body)
    }

    /**
     * Классификатор ОКЭД просится страницами.
     *
     * Прежде запрашивались первые пятьдесят и только они: доскроллить
     * до своего вида деятельности владелец не мог ни при каком запросе,
     * а сколько их всего — не знал никто, в том числе и приложение.
     */
    @Test
    fun `виды деятельности просятся со смещением и общим числом`() {
        val sent = mutableListOf<HttpRequestData>()
        val client = capturing(sent, """{"items":[{"code":"47111","name":"Торговля"}],"total":2107}""")
        val answer = runBlocking { client.company.okeds("торг", from = 50) }

        val url = sent.single().url.toString()
        assertTrue(url.contains("offset=50"), url)
        assertTrue(url.contains("limit=$OKED_PAGE"), url)
        assertEquals(2107, answer.total)
    }

    /** Строка запроса, с которой ушли отчёты за срок [period]. */
    private fun reportsAsked(period: DocumentPeriod): String {
        val sent = mutableListOf<HttpRequestData>()
        runBlocking { capturing(sent, EMPTY_PAGE).documents.reports("id", page = 0, period = period) }
        return sent.single().url.encodedQuery
    }

    private fun capturing(sent: MutableList<HttpRequestData>, body: String): CabinetPorts {
        val engine = MockEngine { request ->
            sent += request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetWire.json) }
        }
        return CabinetWire(http = http).signedPorts()
    }

    private companion object {
        const val EMPTY_PAGE = """{"page":0,"size":50,"totalElements":0,"items":[]}"""
    }
}
