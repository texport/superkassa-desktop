package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetFake.Reply
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Торговая сводка: согласованный вид ответа и снисходительность разбора —
 * иное имя поля, другой конверт и пустое тело не валят экран.
 */
class CabinetSalesTest {

    private val period = SalesFilter(from = "2026-09-01", to = "2026-09-20")

    @Test
    fun summaryIsReadWithPayments() = runTest {
        val fake = CabinetFake.always(
            """{"receiptCount":128,"revenue":1284560.00,"refunds":12400.50,"difference":1272159.50,
               "averageReceipt":10035.62,"tax":137631.43,
               "payments":{"cash":400000.00,"card":800560.00,"mobile":84000.00,"credit":0,"tare":0,"other":0},
               "offlineCount":3,"queuedCount":2,"unknownCount":5,"cashRegisterCount":7,"openShiftCount":4}"""
        )
        val summary = fake.cabinet().analytics.summary(period)

        assertEquals(128, summary.receiptCount)
        assertEquals(128_456_000L, summary.revenue?.tiyn())
        assertEquals(127_215_950L, summary.difference?.tiyn())
        assertEquals(80_056_000L, summary.payments.card?.tiyn())
        assertEquals(4, summary.openShiftCount)
        assertEquals("/api/analytics/sales/summary?from=2026-09-01&to=2026-09-20", fake.asked.single().target())
    }

    @Test
    fun otherFieldNamesAndUnknownKeysDoNotBreakReading() = runTest {
        val summary = CabinetFake.always(
            """{"receipts":12,"total":5000.00,"returns":500.00,"taxTotal":536.00,"net":"4500.00",
               "paymentTypes":{"cash":5000.00,"mobileMoney":"10"},"openShifts":1,"buyCount":3,"buys":7500.00,
               "somethingNew":{"deep":[1,2]}}"""
        ).cabinet().analytics.summary(period)

        assertEquals(12, summary.receiptCount)
        assertEquals(500_000L, summary.revenue?.tiyn())
        assertEquals(50_000L, summary.refunds?.tiyn())
        assertEquals(450_000L, summary.difference?.tiyn())
        assertEquals(1_000L, summary.payments.mobile?.tiyn())
        assertEquals(3, summary.purchaseCount)
        assertEquals(750_000L, summary.purchases?.tiyn())
    }

    @Test
    fun emptySummaryIsZerosNotRefusal() = runTest {
        val summary = CabinetFake.always("{}").cabinet().analytics.summary(period)

        assertEquals(0, summary.receiptCount)
        assertNull(summary.revenue)
        assertNull(summary.purchases)
    }

    /** Имя конверта частью договора не считается: однажды оно стоило окна с исключением разбора. */
    @Test
    fun daysAreReadBareOrInAnyEnvelope() = runTest {
        val rows = """[{"date":"2026-09-19","receiptCount":12,"revenue":145000.00,"refunds":0}]"""
        for (body in listOf(rows, """{"days":$rows}""", """{"items":$rows}""")) {
            val day = CabinetFake.always(body).cabinet().analytics.byDay(period).single()
            assertEquals("2026-09-19", day.date)
            assertEquals(14_500_000L, day.revenue?.tiyn())
        }
        assertTrue(CabinetFake.always("""{"days":null}""").cabinet().analytics.byDay(period).isEmpty())
    }

    @Test
    fun hoursRegistersAndPlacesHaveOwnPaths() = runTest {
        val fake = CabinetFake { request ->
            Reply(
                when (request.url.encodedPath.substringAfterLast('/')) {
                    "by-hour" -> """[{"hour":14,"receipts":9,"total":90000.00}]"""
                    "by-cash-register" -> BY_REGISTER
                    else -> """[{"retailPlaceId":"p1","retailPlaceName":"Магазин на Абая","receipts":40}]"""
                }
            )
        }
        val analytics = fake.cabinet().analytics
        val register = analytics.byRegister(period.copy(retailPlaceId = "p 1", cashRegisterId = "c1")).single()

        assertEquals(9, analytics.byHour(period).single().receiptCount)
        assertEquals("000000010001", register.registrationNumber)
        assertEquals(49_500_000L, register.difference?.tiyn())
        assertEquals("p1", analytics.byPlace(period).single().id)
        assertTrue(fake.asked[0].target().endsWith("to=2026-09-20&retailPlaceId=p+1&cashRegisterId=c1"))
    }

    /** Все разрезы разом — шесть ручек с одним отбором. */
    @Test
    fun salesAskSixHandlesWithOneFilter() = runTest {
        val fake = CabinetFake { request ->
            val path = request.url.encodedPath
            Reply(
                when {
                    path.endsWith("documents") -> DELIVERY
                    path.endsWith("summary") -> "{}"
                    else -> "[]"
                }
            )
        }
        val figures = fake.cabinet().analytics.sales(period)

        assertEquals(6, fake.asked.size)
        assertTrue(fake.asked.all { it.url.parameters["from"] == "2026-09-01" })
        assertEquals(300, figures.delivery.receipts.delivered)
        assertEquals(5, figures.delivery.receipts.queued)
        assertEquals(7, figures.delivery.offlineCount)
    }

    private companion object {
        const val BY_REGISTER = """{"rows":[{"cashRegisterId":"c1","rnm":"000000010001","internalName":"Касса у входа",
            "retailPlace":"Магазин на Абая","receipts":40,"total":500000.00,"netRevenue":495000.00}]}"""

        const val DELIVERY = """{"period":{"from":"2026-09-13","to":"2026-09-20"},
            "receipts":{"total":312,"delivered":300,"inQueue":5,"unknown":5,"rejected":2},
            "reports":{"total":10,"delivered":10,"queued":0,"unknown":0,"rejected":0},"offline":7}"""
    }
}
