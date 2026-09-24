package kz.mybrain.superkassa.data.analytics

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.model.troubleOrNull
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Разрезы торговой сводки — сутки, часы, кассы, точки и доставка —
 * доходят до предметной области одним ответом.
 *
 * Перечни кабинет отдаёт по-разному: голым массивом, конвертом с любым
 * именем. Частичного ответа нет: отказ одной ручки — помеха всей сводки.
 */
class AnalyticsSalesContractTest {

    private val period = SalesFilter(from = "2026-09-01", to = "2026-09-20")

    /** Все шесть ручек: ручка [slice] отвечает [body], прочие — пусто. */
    private fun cabinet(slice: String, body: String) = CabinetReplies.answering { target ->
        val path = target.substringBefore('?')
        when {
            path.endsWith(slice) -> body
            path.endsWith("/summary") || path.endsWith("/documents") -> "{}"
            else -> "[]"
        }
    }

    private fun figures(slice: String, body: String): SalesFigures =
        requireNotNull(runBlocking { cabinet(slice, body).analytics.sales(period) }.valueOrNull())

    @Test
    fun `сутки читаются и голым массивом, и из конверта с любым именем`() {
        val rows = """[{"date":"2026-09-19","receiptCount":12,"revenue":145000.00,"refunds":0}]"""
        listOf(rows, """{"days":$rows}""", """{"items":$rows}""").forEach { body ->
            val day = figures("/by-day", body).days.single()
            assertEquals("2026-09-19", day.date)
            assertEquals(12, day.receiptCount)
            assertEquals(tiyn("145000.00"), day.revenue)
        }
    }

    @Test
    fun `часы, кассы и точки разбираются своими ручками`() {
        val hour = figures("/by-hour", """[{"hour":14,"receipts":9,"total":90000.00}]""").hours.single()
        assertEquals(14, hour.hour)
        assertEquals(9, hour.receiptCount)

        val register = figures(
            "/by-cash-register",
            """{"rows":[{"cashRegisterId":"c1","registrationNumber":"KGD-2000302",
               "internalName":"Касса у входа","retailPlace":"Магазин на Абая","receipts":40,
               "total":500000.00,"netRevenue":495000.00,"lastSeen":"2026-09-19T08:14:00Z"}]}"""
        ).registers.single()
        assertEquals("Касса у входа", register.name)
        assertEquals("Магазин на Абая", register.retailPlaceName)
        assertEquals(tiyn("495000.00"), register.difference)

        val place = figures(
            "/by-retail-place",
            """[{"retailPlaceId":"p1","retailPlaceName":"Магазин на Абая","receipts":40}]"""
        ).places.single()
        assertEquals("p1", place.id)
    }

    @Test
    fun `шесть разрезов спрашиваются одним отбором`() {
        val cabinet = cabinet("/summary", "{}")
        runBlocking { cabinet.analytics.sales(period) }
        val slices = cabinet.asked.map { it.substringBefore('?').substringAfterLast('/') }.toSet()
        assertEquals(setOf("summary", "by-day", "by-hour", "by-cash-register", "by-retail-place", "documents"), slices)
        assertTrue(cabinet.asked.all { it.endsWith("?from=2026-09-01&to=2026-09-20") }, cabinet.asked.toString())
    }

    /**
     * Кабинет считает чеки и отчёты порознь; на плитках они складываются:
     * владельцу важно, что не доехало, а вид документа виден в журнале.
     */
    @Test
    fun `состояние доставки складывается по видам документов`() {
        val delivery = figures(
            "/documents",
            """{"period":{"from":"2026-09-13","to":"2026-09-20"},
               "receipts":{"total":312,"delivered":300,"queued":5,"unknown":5,"rejected":2},
               "reports":{"total":10,"delivered":10,"queued":0,"unknown":0,"rejected":0},
               "offlineCount":7}"""
        ).delivery
        assertEquals(310, delivery.delivered)
        assertEquals(5, delivery.unknown)
        assertEquals(5, delivery.queued)
        assertEquals(2, delivery.rejected)
        assertEquals(7, delivery.offline)
    }

    @Test
    fun `404 одной ручки до выкладки кабинета — отсутствующий раздел на всю сводку`() {
        val cabinet = CabinetReplies.answering { target ->
            when {
                target.contains("/by-day") -> null
                target.contains("/summary") || target.contains("/documents") -> "{}"
                else -> "[]"
            }
        }
        assertEquals(AnalyticsTrouble.NotDeployed, runBlocking { cabinet.analytics.sales(period) }.troubleOrNull())
    }

    /** Сумма кабинета в тиынах — той же записью, что читает модуль кабинета. */
    private fun tiyn(tenge: String): Long = CabinetDecimal.of(tenge).tiyn()
}
