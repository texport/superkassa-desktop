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
import kz.mybrain.superkassa.desktop.server.cabinet.SalesFilter
import kz.mybrain.superkassa.desktop.server.cabinet.salesByCashRegister
import kz.mybrain.superkassa.desktop.server.cabinet.salesByDay
import kz.mybrain.superkassa.desktop.server.cabinet.salesByHour
import kz.mybrain.superkassa.desktop.server.cabinet.salesByRetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.salesDelivery
import kz.mybrain.superkassa.desktop.server.cabinet.salesSummary
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsTrouble
import kz.mybrain.superkassa.desktop.ui.analytics.analyticsTrouble
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ответы торговой сводки разбираются в модели приложения.
 *
 * Сторону кабинета пишут прямо сейчас, и до её выкладки этот тест —
 * единственное место, где договор проверяется целиком. Проверяется
 * не только согласованный вид ответа, но и снисходительность разбора:
 * незнакомое поле, другое имя конверта и пустое тело не должны валить
 * экран.
 */
class AnalyticsSalesContractTest {

    private var seenPath: String = ""

    private val period = SalesFilter(
        from = LocalDate.parse("2026-09-01"),
        to = LocalDate.parse("2026-09-20")
    )

    private fun clientReturning(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetClient {
        val engine = MockEngine { request ->
            seenPath = request.url.encodedPath + "?" + request.url.encodedQuery
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(CabinetClient.lenientJson) }
        }
        return CabinetClient(http = http)
    }

    @Test
    fun `главные числа срока разбираются вместе с видами расчётов`() {
        val client = clientReturning(
            """{"receiptCount":128,"revenue":1284560.00,"refunds":12400.50,"difference":1272159.50,
               "averageReceipt":10035.62,"tax":137631.43,
               "payments":{"cash":400000.00,"card":800560.00,"mobile":84000.00,
                 "credit":0,"tare":0,"other":0},
               "offlineCount":3,"queuedCount":2,"unknownCount":5,"cashRegisterCount":7,"openShiftCount":4}"""
        )
        val summary = runBlocking { client.salesSummary("token", period) }
        assertEquals(128, summary.receiptCount)
        assertEquals(BigDecimal("1284560.00"), summary.revenue)
        assertEquals(BigDecimal("1272159.50"), summary.net)
        assertEquals(BigDecimal("800560.00"), summary.payments.card)
        assertEquals(7, summary.cashRegisterCount)
        assertEquals(4, summary.openShiftCount)
        assertTrue(seenPath.contains("from=2026-09-01"), seenPath)
        assertTrue(seenPath.contains("to=2026-09-20"), seenPath)
    }

    @Test
    fun `иначе названные поля и незнакомый ключ не валят разбор`() {
        val client = clientReturning(
            """{"receipts":12,"total":5000.00,"returns":500.00,"taxTotal":536.00,
               "paymentTypes":{"cash":5000.00},"openShifts":1,"somethingNew":{"deep":[1,2]}}"""
        )
        val summary = runBlocking { client.salesSummary("token", period) }
        assertEquals(12, summary.receiptCount)
        assertEquals(BigDecimal("5000.00"), summary.revenue)
        assertEquals(BigDecimal("500.00"), summary.refunds)
        assertEquals(BigDecimal("536.00"), summary.tax)
        assertEquals(1, summary.openShiftCount)
        assertEquals(BigDecimal("5000.00"), summary.payments.cash)
    }

    @Test
    fun `пустое тело сводки читается нулями, а не отказом`() {
        val client = clientReturning("{}")
        val summary = runBlocking { client.salesSummary("token", period) }
        assertEquals(0, summary.receiptCount)
        assertEquals(BigDecimal.ZERO, summary.net)
        assertEquals(BigDecimal.ZERO, summary.average)
        assertTrue(!summary.purchased, "покупок в ответе нет — показывать нечего")
    }

    @Test
    fun `покупка у населения приходит своими числами и в выручку не входит`() {
        val client = clientReturning(
            """{"receiptCount":10,"revenue":50000.00,"refunds":1000.00,
               "purchaseCount":3,"purchases":7500.00,"purchaseRefunds":250.00}"""
        )
        val summary = runBlocking { client.salesSummary("token", period) }
        assertEquals(3, summary.purchaseCount)
        assertEquals(BigDecimal("7500.00"), summary.purchases)
        assertEquals(BigDecimal("250.00"), summary.purchaseRefunds)
        assertTrue(summary.purchased)
        assertEquals(BigDecimal("50000.00"), summary.revenue, "покупка в выручку не подмешана")
        assertEquals(BigDecimal("49000.00"), summary.net, "разность считается по продажам")
    }

    @Test
    fun `невыложенный кабинет покупок не присылает, и сводка читается как прежде`() {
        val client = clientReturning(
            """{"receiptCount":10,"revenue":50000.00,"refunds":1000.00,"tax":5000.00}"""
        )
        val summary = runBlocking { client.salesSummary("token", period) }
        assertEquals(0, summary.purchaseCount)
        assertEquals(null, summary.purchases)
        assertEquals(null, summary.purchaseRefunds)
        assertTrue(!summary.purchased, "карточки покупки на экране не будет")
        assertEquals(BigDecimal("50000.00"), summary.revenue)
    }

    @Test
    fun `сутки читаются и голым массивом, и из конверта с любым именем`() {
        val rows = """[{"date":"2026-09-19","receiptCount":12,"revenue":145000.00,"refunds":0}]"""
        val bare = runBlocking { clientReturning(rows).salesByDay("token", period) }
        val wrapped = runBlocking { clientReturning("""{"days":$rows}""").salesByDay("token", period) }
        val renamed = runBlocking { clientReturning("""{"items":$rows}""").salesByDay("token", period) }
        listOf(bare, wrapped, renamed).forEach { days ->
            assertEquals("2026-09-19", days.single().date)
            assertEquals(12, days.single().receiptCount)
            assertEquals(BigDecimal("145000.00"), days.single().revenue)
        }
    }

    @Test
    fun `часы, кассы и точки разбираются своими ручками`() {
        val hours = runBlocking {
            clientReturning("""[{"hour":14,"receipts":9,"total":90000.00}]""").salesByHour("token", period)
        }
        assertEquals(14, hours.single().hour)
        assertEquals(9, hours.single().receiptCount)

        val registers = runBlocking {
            clientReturning(
                """{"rows":[{"cashRegisterId":"c1","registrationNumber":"KGD-2000302",
                   "internalName":"Касса у входа","retailPlace":"Магазин на Абая","receipts":40,
                   "total":500000.00,"netRevenue":495000.00,"lastSeen":"2026-09-19T08:14:00Z"}]}"""
            ).salesByCashRegister("token", period)
        }
        assertEquals("Касса у входа", registers.single().name)
        assertEquals("Магазин на Абая", registers.single().retailPlaceName)
        assertEquals(BigDecimal("495000.00"), registers.single().difference)
        assertTrue(seenPath.startsWith("/api/analytics/sales/by-cash-register"), seenPath)

        val places = runBlocking {
            clientReturning("""[{"retailPlaceId":"p1","retailPlaceName":"Магазин на Абая","receipts":40}]""")
                .salesByRetailPlace("token", period)
        }
        assertEquals("p1", places.single().id)
        assertTrue(seenPath.startsWith("/api/analytics/sales/by-retail-place"), seenPath)
    }

    /**
     * Кабинет считает чеки и отчёты порознь; на плитках они складываются:
     * владельцу важно, что не доехало, а вид документа виден в журнале.
     */
    @Test
    fun `состояние доставки складывается по видам документов`() {
        val client = clientReturning(
            """{"period":{"from":"2026-09-13","to":"2026-09-20"},
               "receipts":{"total":312,"delivered":300,"queued":5,"unknown":5,"rejected":2},
               "reports":{"total":10,"delivered":10,"queued":0,"unknown":0,"rejected":0},
               "offlineCount":7}"""
        )
        val delivery = runBlocking { client.salesDelivery("token", period) }
        assertEquals(310, delivery.delivered)
        assertEquals(5, delivery.unknown)
        assertEquals(5, delivery.queued)
        assertEquals(2, delivery.rejected)
        assertEquals(7, delivery.offline)
        assertTrue(seenPath.startsWith("/api/analytics/sales/documents"), seenPath)
    }

    /** Разность кабинет называет net; без неё она считается из выручки. */
    @Test
    fun `разность и электронные деньги разбираются именами кабинета`() {
        val client = clientReturning(
            """{"receiptCount":4,"revenue":"1000.00","refunds":"250.00","net":"750.00",
               "payments":{"cash":"400.00","card":"300.00","electronic":"300.00"}}"""
        )
        val summary = runBlocking { client.salesSummary("token", period) }
        assertEquals(BigDecimal("750.00"), summary.net)
        assertEquals(BigDecimal("300.00"), summary.payments.electronic)
    }

    @Test
    fun `отбор по точке и кассе уходит строкой запроса, а незаданный не пишется`() {
        val client = clientReturning("{}")
        runBlocking {
            client.salesSummary("token", period.copy(retailPlaceId = "p 1", cashRegisterId = "c1"))
        }
        assertTrue(seenPath.contains("retailPlaceId=p+1"), seenPath)
        assertTrue(seenPath.contains("cashRegisterId=c1"), seenPath)
        runBlocking { client.salesSummary("token", period) }
        assertTrue(!seenPath.contains("retailPlaceId"), seenPath)
    }

    @Test
    fun `404 до выкладки кабинета — не отказ, а отсутствующий раздел`() {
        val client = clientReturning("""{"title":"Not Found"}""", HttpStatusCode.NotFound)
        val trouble = runCatching {
            runBlocking { client.salesSummary("token", period) }
        }.exceptionOrNull()?.let(::analyticsTrouble)
        assertEquals(AnalyticsTrouble.NotDeployed, trouble)

        val listTrouble = runCatching {
            runBlocking { client.salesByDay("token", period) }
        }.exceptionOrNull()?.let(::analyticsTrouble)
        assertEquals(AnalyticsTrouble.NotDeployed, listTrouble)
    }
}
