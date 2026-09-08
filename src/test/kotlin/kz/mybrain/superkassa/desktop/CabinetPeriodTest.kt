package kz.mybrain.superkassa.desktop

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
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetClient
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentPeriod
import kz.mybrain.superkassa.desktop.server.cabinet.ReceiptSearch
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovements
import kz.mybrain.superkassa.desktop.server.cabinet.receipts
import kz.mybrain.superkassa.desktop.server.cabinet.reports
import kz.mybrain.superkassa.desktop.ui.cabinet.DocumentKind
import kz.mybrain.superkassa.desktop.ui.cabinet.DocumentSpan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Срок, за который кабинет отдаёт документы.
 *
 * Проверяется то, из-за чего раздел показал бы не тот период: граница
 * считается от начала суток рабочего места, а не «минус столько-то
 * часов», и «сегодня» обязано включать смену, открытую утром.
 */
class CabinetPeriodTest {

    private val almaty = ZoneId.of("Asia/Almaty")

    @Test
    fun `сегодня начинается с полуночи рабочего места`() {
        val period = DocumentSpan.Today.period(almaty)
        assertEquals(LocalDate.now(almaty).atStartOfDay(almaty).toInstant(), period.from)
        assertNull(period.to, "верхней границы у срока нет: свежее приходит и сейчас")
    }

    @Test
    fun `неделя включает сегодняшний день и шесть прошлых`() {
        val period = DocumentSpan.Week.period(almaty)
        val expected = LocalDate.now(almaty).minusDays(6).atStartOfDay(almaty).toInstant()
        assertEquals(expected, period.from)
    }

    @Test
    fun `месяц включает сегодняшний день и двадцать девять прошлых`() {
        val period = DocumentSpan.Month.period(almaty)
        val expected = LocalDate.now(almaty).minusDays(29).atStartOfDay(almaty).toInstant()
        assertEquals(expected, period.from)
    }

    @Test
    fun `у всего времени границ нет`() {
        val period = DocumentSpan.All.period(almaty)
        assertNull(period.from)
        assertNull(period.to)
        assertEquals("", period.query(), "пустой срок не добавляет параметров в запрос")
    }

    @Test
    fun `границы уходят параметрами запроса`() {
        val from = Instant.parse("2026-09-01T00:00:00Z")
        val to = Instant.parse("2026-09-08T00:00:00Z")
        assertEquals(
            "&dateFrom=2026-09-01T00:00:00Z&dateTo=2026-09-08T00:00:00Z",
            DocumentPeriod(from, to).query()
        )
    }

    @Test
    fun `одна граница не тянет за собой вторую`() {
        assertEquals("&dateFrom=2026-09-01T00:00:00Z", DocumentPeriod(Instant.parse("2026-09-01T00:00:00Z")).query())
        assertEquals("&dateTo=2026-09-08T00:00:00Z", DocumentPeriod(to = Instant.parse("2026-09-08T00:00:00Z")).query())
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
        val period = DocumentPeriod(Instant.parse("2026-09-01T00:00:00Z"))
        runBlocking {
            client.receipts("token", "id", ReceiptSearch(dateFrom = period.fromText(), dateTo = period.toText()))
        }
        val body = (sent.single().body as TextContent).text
        assertTrue(body.contains("\"dateFrom\":\"2026-09-01T00:00:00Z\""), body)
    }

    @Test
    fun `границы срока уходят параметрами у отчётов и движения денег`() {
        val sent = mutableListOf<HttpRequestData>()
        val client = capturing(sent, EMPTY_PAGE)
        val period = DocumentPeriod(Instant.parse("2026-09-01T00:00:00Z"))
        runBlocking {
            client.reports("token", "id", page = 0, period = period)
            client.cashMovements("token", "id", page = 0, period = period)
        }
        sent.forEach { assertTrue(it.url.toString().contains("dateFrom=2026-09-01T00:00:00Z"), it.url.toString()) }
    }

    private fun capturing(sent: MutableList<HttpRequestData>, body: String): CabinetClient {
        val engine = MockEngine { request ->
            sent += request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    private companion object {
        const val EMPTY_PAGE = """{"page":0,"size":50,"totalElements":0,"items":[]}"""
    }
}
