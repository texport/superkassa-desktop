package kz.mybrain.superkassa.domain.analytics.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Прошлый срок сводки: той же длины, вплотную перед нынешним.
 */
class SalesSpanTest {

    // --- Прошлый срок ---

    @Test
    fun `прошлый срок той же длины стоит перед нынешним`() {
        val week = SalesSpan(LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-20"))
        val before = week.previous()
        assertEquals(LocalDate.parse("2026-09-07"), before.from)
        assertEquals(LocalDate.parse("2026-09-13"), before.to)
    }

    @Test
    fun `для одного дня прошлый срок — вчера`() {
        val day = LocalDate.parse("2026-09-20")
        val before = SalesSpan(day, day).previous()
        assertEquals(day.minus(1, DateTimeUnit.DAY), before.from)
        assertEquals(day.minus(1, DateTimeUnit.DAY), before.to)
    }

    @Test
    fun `отбор по кассе прошлый срок не теряет`() {
        val day = LocalDate.parse("2026-09-20")
        val before = SalesSpan(day, day).previous().filter(register = "c1")
        assertEquals("c1", before.cashRegisterId)
    }
}
