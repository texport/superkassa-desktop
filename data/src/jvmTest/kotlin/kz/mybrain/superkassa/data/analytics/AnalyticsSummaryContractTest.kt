package kz.mybrain.superkassa.data.analytics

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.model.troubleOrNull
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Главные числа срока доходят до предметной области в тиынах.
 *
 * Сторону кабинета пишут прямо сейчас: проверяется не только согласованный
 * вид ответа, но и снисходительность разбора — незнакомое поле, другое имя
 * поля и пустое тело не должны валить экран.
 */
class AnalyticsSummaryContractTest {

    private val period = SalesFilter(from = "2026-09-01", to = "2026-09-20")

    private fun summary(cabinet: CabinetReplies, filter: SalesFilter = period): SalesSummary =
        requireNotNull(runBlocking { cabinet.analytics.summary(filter) }.valueOrNull())

    private fun summaryOf(body: String): SalesSummary = summary(CabinetReplies.always(body))

    @Test
    fun `главные числа срока разбираются вместе с видами расчётов`() {
        val cabinet = CabinetReplies.always(
            """{"receiptCount":128,"revenue":1284560.00,"refunds":12400.50,"difference":1272159.50,
               "averageReceipt":10035.62,"tax":137631.43,
               "payments":{"cash":400000.00,"card":800560.00,"mobile":84000.00,
                 "credit":0,"tare":0,"other":0},
               "offlineCount":3,"queuedCount":2,"unknownCount":5,"cashRegisterCount":7,"openShiftCount":4}"""
        )
        val summary = summary(cabinet)
        assertEquals(128, summary.receiptCount)
        assertEquals(tiyn("1284560.00"), summary.revenue)
        assertEquals(tiyn("1272159.50"), summary.net)
        assertEquals(tiyn("800560.00"), summary.payments.card)
        assertEquals(7, summary.cashRegisterCount)
        assertEquals(4, summary.openShiftCount)
        assertEquals("/api/analytics/sales/summary?from=2026-09-01&to=2026-09-20", cabinet.asked.single())
    }

    @Test
    fun `иначе названные поля и незнакомый ключ не валят разбор`() {
        val summary = summaryOf(
            """{"receipts":12,"total":5000.00,"returns":500.00,"taxTotal":536.00,
               "paymentTypes":{"cash":5000.00},"openShifts":1,"somethingNew":{"deep":[1,2]}}"""
        )
        assertEquals(12, summary.receiptCount)
        assertEquals(tiyn("5000.00"), summary.revenue)
        assertEquals(tiyn("500.00"), summary.refunds)
        assertEquals(tiyn("536.00"), summary.tax)
        assertEquals(1, summary.openShiftCount)
        assertEquals(tiyn("5000.00"), summary.payments.cash)
    }

    @Test
    fun `пустое тело сводки читается нулями, а не отказом`() {
        val summary = summaryOf("{}")
        assertEquals(0, summary.receiptCount)
        assertEquals(0L, summary.net)
        assertEquals(0L, summary.average)
        assertTrue(!summary.purchased, "покупок в ответе нет — показывать нечего")
    }

    @Test
    fun `покупка у населения приходит своими числами и в выручку не входит`() {
        val summary = summaryOf(
            """{"receiptCount":10,"revenue":50000.00,"refunds":1000.00,
               "purchaseCount":3,"purchases":7500.00,"purchaseRefunds":250.00}"""
        )
        assertEquals(3, summary.purchaseCount)
        assertEquals(tiyn("7500.00"), summary.purchases)
        assertEquals(tiyn("250.00"), summary.purchaseRefunds)
        assertTrue(summary.purchased)
        assertEquals(tiyn("50000.00"), summary.revenue, "покупка в выручку не подмешана")
        assertEquals(tiyn("49000.00"), summary.net, "разность считается по продажам")
    }

    @Test
    fun `невыложенный кабинет покупок не присылает, и сводка читается как прежде`() {
        val summary = summaryOf("""{"receiptCount":10,"revenue":50000.00,"refunds":1000.00,"tax":5000.00}""")
        assertEquals(0, summary.purchaseCount)
        assertNull(summary.purchases)
        assertNull(summary.purchaseRefunds)
        assertTrue(!summary.purchased, "карточки покупки на экране не будет")
        assertEquals(tiyn("50000.00"), summary.revenue)
    }

    /** Деньги кабинета — десятичные тенге; в предметной области — точные тиыны. */
    @Test
    fun `суммы кабинета доходят в тиынах точно`() {
        val summary = summaryOf("""{"revenue":61825.00,"refunds":7204.00,"averageReceipt":753.96,"taxTotal":0.00}""")
        assertEquals(6_182_500L, summary.revenue)
        assertEquals(5_462_100L, summary.net)
        assertEquals(75_396L, summary.average)
        assertEquals(0L, summary.tax)
    }

    /** Разность кабинет называет net; без неё она считается из выручки. */
    @Test
    fun `разность и электронные деньги разбираются именами кабинета`() {
        val summary = summaryOf(
            """{"receiptCount":4,"revenue":"1000.00","refunds":"250.00","net":"750.00",
               "payments":{"cash":"400.00","card":"300.00","electronic":"300.00"}}"""
        )
        assertEquals(tiyn("750.00"), summary.net)
        assertEquals(tiyn("300.00"), summary.payments.electronic)
    }

    @Test
    fun `отбор по точке и кассе уходит строкой запроса, а незаданный не пишется`() {
        val cabinet = CabinetReplies.always("{}")
        summary(cabinet, period.copy(retailPlaceId = "p 1", cashRegisterId = "c1"))
        summary(cabinet)
        assertTrue(cabinet.asked[0].endsWith("retailPlaceId=p+1&cashRegisterId=c1"), cabinet.asked[0])
        assertTrue(!cabinet.asked[1].contains("retailPlaceId"), cabinet.asked[1])
    }

    @Test
    fun `404 до выкладки кабинета — не отказ, а отсутствующий раздел`() {
        val cabinet = CabinetReplies.always("""{"title":"Not Found"}""", status = 404)
        assertEquals(AnalyticsTrouble.NotDeployed, runBlocking { cabinet.analytics.summary(period) }.troubleOrNull())
    }

    /** Сумма кабинета в тиынах — той же записью, что читает модуль кабинета. */
    private fun tiyn(tenge: String): Long = CabinetDecimal.of(tenge).tiyn()
}
