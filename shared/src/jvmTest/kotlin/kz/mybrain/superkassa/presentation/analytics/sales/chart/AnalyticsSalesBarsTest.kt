package kz.mybrain.superkassa.presentation.analytics.sales.chart

import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kz.mybrain.superkassa.domain.analytics.model.SalesDay
import kz.mybrain.superkassa.domain.analytics.model.SalesHour
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.tiynOf
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Столбики и столбцы сводки: сутки срока по местам, часы — все двадцать
 * четыре, высота — доля от наибольшего.
 */
class AnalyticsSalesBarsTest {

    private val texts = analyticsTexts(Language.Ru).sales

    private fun sum(value: String): Long = tiynOf(value)

    // --- Столбики ---

    @Test
    fun `суток столько, сколько в сроке, а часов всегда двадцать четыре`() {
        val one = SalesSpan(LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-19"))
        val days = dayBars(listOf(SalesDay(date = "2026-09-19", receiptCount = 2, revenue = sum("10.00"))), one, texts)
        assertEquals("19.09", days.single().label)
        assertTrue(days.single().caption.contains("19.09.2026"), days.single().caption)

        val hours = hourBars(listOf(SalesHour(hour = 14, receiptCount = 9, revenue = sum("90.00"))), texts)
        assertEquals(24, hours.size)
        assertEquals(sum("90.00"), hours[14].value)
        assertEquals(0L, hours[0].value)
        assertTrue(hours[14].caption.startsWith("14:00"), hours[14].caption)
    }

    @Test
    fun `пустые сутки срока достраиваются нулём на своих местах`() {
        val week = SalesSpan(LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-20"))
        val bars = dayBars(
            listOf(
                SalesDay(date = "2026-09-15", receiptCount = 4, revenue = sum("400.00")),
                SalesDay(date = "2026-09-17", receiptCount = 1, revenue = sum("100.00")),
                SalesDay(date = "2026-09-20", receiptCount = 9, revenue = sum("900.00"))
            ),
            week,
            texts
        )
        assertEquals(7, bars.size, bars.joinToString { it.label })
        assertEquals(listOf("14.09", "15.09", "16.09", "17.09", "18.09", "19.09", "20.09"), bars.map { it.label })
        assertEquals(0L, bars.first().value)
        assertEquals(sum("400.00"), bars[1].value)
        assertEquals(0L, bars[2].value)
        assertEquals(sum("900.00"), bars.last().value)
    }

    @Test
    fun `сутки вне срока в ряд не попадают, а порядок остаётся временным`() {
        val twoDays = SalesSpan(LocalDate.parse("2026-09-19"), LocalDate.parse("2026-09-20"))
        val bars = dayBars(
            listOf(
                SalesDay(date = "2026-09-20", receiptCount = 2, revenue = sum("20.00")),
                SalesDay(date = "2026-08-01", receiptCount = 7, revenue = sum("70.00")),
                SalesDay(date = "2026-09-19", receiptCount = 1, revenue = sum("10.00"))
            ),
            twoDays,
            texts
        )
        assertEquals(listOf("19.09", "20.09"), bars.map { it.label })
        assertEquals(sum("10.00"), bars.first().value)
    }

    @Test
    fun `пустые сутки подписаны нулём чеков, а не прочерком`() {
        val oneDay = SalesSpan(LocalDate.parse("2026-09-18"), LocalDate.parse("2026-09-18"))
        val caption = dayBars(emptyList(), oneDay, texts).single().caption
        assertTrue(caption.contains("18.09.2026"), caption)
        assertTrue(caption.contains("${texts.receipts}: 0"), caption)
    }

    @Test
    fun `высота столбика — доля от наибольшего, а ноль не рисуется`() {
        assertEquals(1f, barShare(sum("100"), sum("100")))
        assertEquals(0.5f, barShare(sum("50"), sum("100")))
        assertEquals(0f, barShare(0L, sum("100")))
        assertEquals(0f, barShare(sum("10"), 0L))
    }

    @Test
    fun `под указателем всегда ровно один столбик, а за полотном — ни одного`() {
        assertEquals(0, barAt(x = 1f, width = 100, count = 10))
        assertEquals(9, barAt(x = 99f, width = 100, count = 10))
        assertEquals(9, barAt(x = 100f, width = 100, count = 10))
        assertNull(barAt(x = -1f, width = 100, count = 10))
        assertNull(barAt(x = 101f, width = 100, count = 10))
        assertNull(barAt(x = 10f, width = 100, count = 0))
    }

    @Test
    fun `ряд короче недели держит деления, а пустое деление столбика не называет`() {
        // Один день занимает седьмую часть полотна, а не всё полотно:
        // заливка от края до края читается не как столбик, а как поломка.
        assertEquals(7, chartSlots(1))
        assertEquals(7, chartSlots(7))
        assertEquals(30, chartSlots(30))
        assertEquals(0, barAt(x = 10f, width = 700, count = 1))
        assertNull(barAt(x = 300f, width = 700, count = 1))
    }

    @Test
    fun `ось подписывается не чаще, чем подписи помещаются`() {
        assertEquals(1, axisStep(7))
        assertEquals(1, axisStep(8))
        assertEquals(4, axisStep(30))
        assertEquals(3, axisStep(24))
    }
}
