package kz.mybrain.superkassa.domain.analytics.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Числа аналитики: одно число считается в одном месте, и считается верно.
 *
 * Каждая проверка закрывает находку ревизии выпуска 1.0.6 — номер стоит
 * в имени проверки.
 */
class AnalyticsNumbersTest {
    private val today = LocalDate.parse("2026-09-23")

    @Test
    fun `116 всё время уходит кабинету его пределом, а не годом`() {
        val all = SalesSpan.of(null, null, today)
        assertEquals(SALES_MOST_DAYS, all.days)
        assertEquals(today, all.to)

        val year = SalesSpan.of(today.minus(1, DateTimeUnit.YEAR), today, today)
        assertEquals(SALES_MOST_DAYS, year.days, "срок длиннее предела не укорочен")
        assertEquals(today, year.to, "укорочен не с давнего конца")
    }

    @Test
    fun `117 идущий срок сравнивается с тем же отрезком прошлого`() {
        val day = SalesSpan.of(today, today, today)
        val week = SalesSpan.of(today.minus(6, DateTimeUnit.DAY), today, today)

        assertEquals(SalesSpan(today.minus(1, DateTimeUnit.DAY), today.minus(1, DateTimeUnit.DAY)), day.previous())
        assertEquals(SalesSpan(today.minus(13, DateTimeUnit.DAY), today.minus(7, DateTimeUnit.DAY)), week.previous())
        val change = overviewOf(SalesSummary(receiptCount = 12), SalesSummary(receiptCount = 10)).receiptsChange
        assertEquals(20, change, "изменение к прошлому сроку не посчитано")
    }

    @Test
    fun `118 нули покупок не делают срок сроком с покупками`() {
        val zeros = SalesSummary(purchases = 0, purchaseRefunds = 0)
        assertFalse(zeros.purchased, "«purchases: 0.00» кабинета сочтено покупкой")
        assertTrue(SalesSummary(purchases = tiynOf("100.00")).purchased)
        assertTrue(SalesSummary(purchaseCount = 1).purchased)
    }

    /** У кассы с открытой сменой смена одна — в учёте, в окне кассы и на карте. */
    @Test
    fun `119 открытые смены считаются по кассам, одним правилом`() {
        val kkms = listOf(
            kkm("c1", "REGISTERED", "OPEN"),
            kkm("c2", "REGISTERED", "OPEN"),
            kkm("c3", "DRAFT", "UNKNOWN"),
            kkm("c4", "DRAFT", " open ")
        )
        assertEquals(3, openShifts(kkms))
        assertEquals(3, recordCount(kkms).openShifts)
        assertEquals(2, recordCount(kkms).trading, "торгующими считаются только кассы на учёте")
        assertEquals(1, openShifts(listOf(kkms.first())), "у одной кассы с открытой сменой — одна смена")
    }

    @Test
    fun `120 и 125 и 127 слова о числах говорят то, что считается`() {
        val ru = textsOf(Language.Ru).analytics
        assertEquals("Касс с продажами", ru.sales.online)
        assertEquals("4 971 из 4 971", ru.mapShownOf.format("4 971", "4 971"))
        Language.entries.forEach { language ->
            val hint = textsOf(language).analytics.sales.deliveryHint
            assertFalse("БФД" in hint || "BFD" in hint, "$language: получателем назван БФД")
        }
        assertTrue("МКК" in textsOf(Language.Kk).analytics.sales.deliveryHint)
        assertTrue("КГД" in ru.sales.deliveryHint)
    }

    @Test
    fun `121 нуль НДС назван, а не голый`() {
        val none = overviewOf(SalesSummary(revenue = tiynOf("1000.00"), tax = 0))
        assertFalse(none.taxCharged)
        assertTrue(overviewOf(SalesSummary(tax = tiynOf("107.14"))).taxCharged)
        Language.entries.forEach { language ->
            val sales = textsOf(language).analytics.sales
            assertTrue(sales.vatNone.isNotBlank() && sales.vatNone != sales.vat, "$language: нуль НДС без слов")
        }
    }

    /** Деньги кабинета — десятичные тенге; в предметной области — точные тиыны. */
    @Test
    fun `суммы кабинета разбираются в тиыны точно`() {
        assertEquals(6_182_500L, tiynOf("61825.00"))
        assertEquals(1_250L, tiynOf("12.5"))
        assertEquals(100_000L, tiynOf("1E+3"))
        assertEquals(1L, tiynOf("0.005"), "половина тиына округляется от нуля")
        assertEquals(-750L, tiynOf("-7.50"))
        assertEquals(9_879_703_110_999L, tiynOf("98797031109.99"))
    }

    @Test
    fun `средний чек без ответа кабинета считается до тиына`() {
        assertEquals(33_333L, SalesSummary(receiptCount = 3, revenue = tiynOf("1000.00")).average)
        assertEquals(66_667L, SalesSummary(receiptCount = 3, revenue = tiynOf("2000.00")).average)
    }

    private fun kkm(id: String, status: String, shift: String) =
        AnalyticsKkm(cashRegisterId = id, status = status, shiftStatus = shift)
}
