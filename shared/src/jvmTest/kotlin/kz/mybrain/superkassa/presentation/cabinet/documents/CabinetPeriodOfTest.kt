package kz.mybrain.superkassa.presentation.cabinet.documents

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.presentation.common.period.workplaceToday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Срок документов кабинета по окну журнала.
 *
 * Проверяется то, из-за чего раздел показал бы не тот период: граница
 * считается от начала суток рабочего места, а не «минус столько-то
 * часов», и «сегодня» обязано включать смену, открытую утром.
 */
class CabinetPeriodOfTest {

    private val almaty = TimeZone.of("Asia/Almaty")

    @Test
    fun `день начинается с полуночи рабочего места`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.Day, workplaceToday(almaty)), almaty)
        assertEquals(workplaceToday(almaty).atStartOfDayIn(almaty).toString(), period.from)
        assertNull(period.to, "у сегодняшнего окна верхней границы нет: свежее приходит и сейчас")
    }

    @Test
    fun `неделя включает сегодняшний день и шесть прошлых`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.Week, workplaceToday(almaty)), almaty)
        val expected = workplaceToday(almaty).minus(6, DateTimeUnit.DAY).atStartOfDayIn(almaty).toString()
        assertEquals(expected, period.from)
    }

    @Test
    fun `месяц включает сегодняшний день и двадцать девять прошлых`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.Month, workplaceToday(almaty)), almaty)
        val expected = workplaceToday(almaty).minus(29, DateTimeUnit.DAY).atStartOfDayIn(almaty).toString()
        assertEquals(expected, period.from)
    }

    @Test
    fun `перелистнутое назад окно кончается своей границей, а не сегодня`() {
        val week = JournalPeriod.of(JournalSpan.Week, LocalDate(2026, 9, 14)).shiftedBy(-1)
        val period = cabinetPeriodOf(week, almaty)

        assertEquals(LocalDate(2026, 9, 1).atStartOfDayIn(almaty).toString(), period.from)
        assertEquals(
            LocalDate(2026, 9, 8).atStartOfDayIn(almaty).toString(),
            period.to,
            "без верхней границы «прошлая неделя» отдавала бы и эту"
        )
    }

    @Test
    fun `у всего времени границ нет`() {
        val period = cabinetPeriodOf(JournalPeriod.of(JournalSpan.All, workplaceToday(almaty)), almaty)
        assertNull(period.from)
        assertNull(period.to)
    }
}
