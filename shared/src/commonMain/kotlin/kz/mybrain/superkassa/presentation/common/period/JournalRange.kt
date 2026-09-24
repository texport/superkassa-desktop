package kz.mybrain.superkassa.presentation.common.period

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.strings.api.journal.HistoryJournalTexts
import kotlin.time.Clock

/**
 * Срок, за который журнал спрашивает документы.
 *
 * Сутки берутся целиком и по часам рабочего места: от начала первого дня
 * до конца последнего. Границы считает [dayRange] — и журнал за день,
 * и журнал за месяц обязаны понимать сутки одинаково.
 */
data class JournalRange(val from: LocalDate, val to: LocalDate) {

    /** Начало первых суток срока по часам рабочего места. */
    fun fromMillis(zone: TimeZone = TimeZone.currentSystemDefault()): Long = dayRange(from, zone).fromMillis

    /** Конец последних суток срока: начало следующего дня. */
    fun toMillis(zone: TimeZone = TimeZone.currentSystemDefault()): Long = dayRange(to, zone).toMillis

    /** Суток в сроке: на столько срок и перелистывается. */
    val days: Long get() = from.daysUntil(to) + 1L

    /** Срок в один день: такой показывается одной датой, а не двумя. */
    val oneDay: Boolean get() = from == to

    /** Тот же срок, сдвинутый на своё же число суток. */
    fun shiftedBy(periods: Long): JournalRange {
        val step = periods * days
        return JournalRange(from.plus(step, DateTimeUnit.DAY), to.plus(step, DateTimeUnit.DAY))
    }
}

/**
 * Длина срока быстрым выбором.
 *
 * Названы теми словами, которыми о сроке спрашивают: день, неделя, месяц.
 * Полей ввода дат нет намеренно — за свежим приходят сегодня и на этой
 * неделе, а разбор давнего случая начинается с номера чека, а не
 * с календаря; перелистнуть срок можно стрелками.
 *
 * У «всего времени» границ нет: касса отдаёт журнал с начала своих записей,
 * кабинету уходит отбор без даты.
 */
enum class JournalSpan(val title: (HistoryJournalTexts) -> String, private val days: Long?) {
    Day({ it.day }, 1),
    Week({ it.spanWeek }, WEEK),
    Month({ it.spanMonth }, MONTH),
    All({ it.spanAll }, null);

    /** Срок такой длины, кончающийся сегодня; `null` — без границ. */
    fun range(today: LocalDate = workplaceToday()): JournalRange? {
        val length = days ?: return null
        return JournalRange(from = today.minus(length - 1, DateTimeUnit.DAY), to = today)
    }
}

/**
 * Окно журнала: какой длины срок и где он стоит.
 *
 * Длина и положение разведены: сегменты выбирают длину — день, неделя,
 * месяц, — а стрелки двигают окно этой длины назад и вперёд. Один набор
 * сегментов на оба дела заставлял бы выбирать «прошлую неделю» из списка,
 * в котором её нет.
 */
data class JournalPeriod(val span: JournalSpan, val range: JournalRange?) {

    /** Есть ли куда листать вперёд: дальше сегодняшнего дня идти некуда. */
    fun hasLater(today: LocalDate = workplaceToday()): Boolean =
        range != null && range.to < today

    /** Окно той же длины, сдвинутое назад или вперёд. */
    fun shiftedBy(periods: Long): JournalPeriod = copy(range = range?.shiftedBy(periods))

    companion object {
        /** Окно выбранной длины, кончающееся сегодня. */
        fun of(span: JournalSpan, today: LocalDate = workplaceToday()): JournalPeriod =
            JournalPeriod(span, span.range(today))
    }
}

/** Срок словами: одной датой, если он в один день, и двумя, если шире. */
fun JournalPeriod.text(texts: HistoryJournalTexts): String {
    val window = range ?: return span.title(texts)
    val from = Dates.day(window.from)
    return if (window.oneDay) from else "$from — ${Dates.day(window.to)}"
}

/** Суток в неделе. */
private const val WEEK = 7L

/** Суток в месяце журнала: ровно четыре недели плюс два дня. */
private const val MONTH = 30L

/** Сутки, за которые касса отдаёт журнал: от начала дня до начала следующего. */
data class DayRange(val fromMillis: Long, val toMillis: Long)

/**
 * Границы выбранного дня по часам этой машины.
 *
 * День берётся местный, а не по Гринвичу: кассир ищет свои вчерашние чеки,
 * а не отрезок суток, сдвинутый на часовой пояс сервера.
 */
fun dayRange(day: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): DayRange = DayRange(
    fromMillis = day.atStartOfDayIn(zone).toEpochMilliseconds(),
    toMillis = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
)

/** Сегодня по часам этой машины: от него считаются сроки журнала. */
fun workplaceToday(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate = Clock.System.todayIn(zone)
