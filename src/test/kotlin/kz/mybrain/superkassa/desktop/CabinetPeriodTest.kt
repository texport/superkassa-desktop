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
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.server.cabinet.ReceiptSearch
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovements
import kz.mybrain.superkassa.desktop.server.cabinet.receipts
import kz.mybrain.superkassa.desktop.server.cabinet.reports
import kz.mybrain.superkassa.desktop.server.cabinet.saveOkeds
import kz.mybrain.superkassa.desktop.ui.cabinet.DocumentKind
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetPeriodOf
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Что кабинет получает от рабочего места: срок документов и виды
 * деятельности.
 *
 * Проверяется то, из-за чего раздел показал бы не тот период: граница
 * считается от начала суток рабочего места, а не «минус столько-то
 * часов», и «сегодня» обязано включать смену, открытую утром.
 *
 * Здесь же признак основного вида деятельности: кабинет ждёт примитив
 * `boolean` и на пропущенном поле отвечает отказом разбора — а пропускался
 * он ровно у неосновных, то есть у всех, кроме первого.
 */
class CabinetPeriodTest {

    private val almaty = ZoneId.of("Asia/Almaty")

    @Test
    fun `день начинается с полуночи рабочего места`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.Day, LocalDate.now(almaty)), almaty)
        assertEquals(LocalDate.now(almaty).atStartOfDay(almaty).toInstant(), period.from)
        assertNull(period.to, "у сегодняшнего окна верхней границы нет: свежее приходит и сейчас")
    }

    @Test
    fun `неделя включает сегодняшний день и шесть прошлых`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.Week, LocalDate.now(almaty)), almaty)
        val expected = LocalDate.now(almaty).minusDays(6).atStartOfDay(almaty).toInstant()
        assertEquals(expected, period.from)
    }

    @Test
    fun `месяц включает сегодняшний день и двадцать девять прошлых`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.Month, LocalDate.now(almaty)), almaty)
        val expected = LocalDate.now(almaty).minusDays(29).atStartOfDay(almaty).toInstant()
        assertEquals(expected, period.from)
    }

    @Test
    fun `перелистнутое назад окно кончается своей границей, а не сегодня`() {
        val week = JournalPeriod.of(JournalSpan.Week, LocalDate.of(2026, 9, 14)).shiftedBy(-1)
        val period = cabinetPeriodOf(week, almaty)

        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(almaty).toInstant(), period.from)
        assertEquals(
            LocalDate.of(2026, 9, 8).atStartOfDay(almaty).toInstant(),
            period.to,
            "без верхней границы «прошлая неделя» отдавала бы и эту"
        )
    }

    @Test
    fun `у всего времени границ нет`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.All, LocalDate.now(almaty)), almaty)
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

    @Test
    fun `неосновной вид деятельности уходит с явным признаком`() {
        val sent = mutableListOf<HttpRequestData>()
        val client = capturing(sent, """{"companyId":"c","okeds":[]}""")
        runBlocking {
            client.saveOkeds(
                "token",
                listOf(Oked("47111", "Розничная торговля", primary = true), Oked("47112", "Ещё один"))
            )
        }
        val body = (sent.single().body as TextContent).text
        assertTrue(body.contains("\"primary\":false"), body)
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
