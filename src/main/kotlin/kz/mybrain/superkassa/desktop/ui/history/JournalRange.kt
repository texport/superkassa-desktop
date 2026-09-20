package kz.mybrain.superkassa.desktop.ui.history

import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Срок, за который журнал спрашивает документы.
 *
 * Сутки берутся целиком и по часам рабочего места: от начала первого дня
 * до конца последнего. Границы считает [dayRange] — и журнал за день,
 * и журнал за месяц обязаны понимать сутки одинаково.
 */
data class JournalRange(val from: LocalDate, val to: LocalDate) {

    /** Начало первых суток срока по часам рабочего места. */
    fun fromMillis(zone: ZoneId = ZoneId.systemDefault()): Long = dayRange(from, zone).fromMillis

    /** Конец последних суток срока: начало следующего дня. */
    fun toMillis(zone: ZoneId = ZoneId.systemDefault()): Long = dayRange(to, zone).toMillis

    /** Суток в сроке: на столько срок и перелистывается. */
    val days: Long get() = ChronoUnit.DAYS.between(from, to) + 1

    /** Срок в один день: такой показывается одной датой, а не двумя. */
    val oneDay: Boolean get() = from == to

    /** Тот же срок, сдвинутый на своё же число суток. */
    fun shiftedBy(periods: Long): JournalRange =
        JournalRange(from.plusDays(periods * days), to.plusDays(periods * days))
}

/**
 * Длина срока быстрым выбором.
 *
 * Названы теми словами, которыми о сроке спрашивают: день, неделя, месяц.
 * Полей ввода дат нет намеренно — за свежим приходят сегодня и на этой
 * неделе, а разбор давнего случая начинается с номера чека, а не
 * с календаря; перелистнуть срок можно стрелками.
 *
 * У «всего времени» границ нет: узлу уходит журнал с начала его записей,
 * кабинету — отбор без даты.
 */
enum class JournalSpan(val title: (HistoryJournalTexts) -> String, private val days: Long?) {
    Day({ it.day }, 1),
    Week({ it.spanWeek }, WEEK),
    Month({ it.spanMonth }, MONTH),
    All({ it.spanAll }, null);

    /** Срок такой длины, кончающийся сегодня; `null` — без границ. */
    fun range(today: LocalDate = LocalDate.now()): JournalRange? {
        val length = days ?: return null
        return JournalRange(from = today.minusDays(length - 1), to = today)
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
    fun hasLater(today: LocalDate = LocalDate.now()): Boolean =
        range != null && range.to < today

    /** Окно той же длины, сдвинутое назад или вперёд. */
    fun shiftedBy(periods: Long): JournalPeriod = copy(range = range?.shiftedBy(periods))

    companion object {
        /** Окно выбранной длины, кончающееся сегодня. */
        fun of(span: JournalSpan, today: LocalDate = LocalDate.now()): JournalPeriod =
            JournalPeriod(span, span.range(today))
    }
}

/** Срок словами: одной датой, если он в один день, и двумя, если шире. */
fun JournalPeriod.text(texts: HistoryJournalTexts): String {
    val window = range ?: return span.title(texts)
    val from = JOURNAL_DAY.format(window.from)
    return if (window.oneDay) from else "$from — ${JOURNAL_DAY.format(window.to)}"
}

/** Суток в неделе. */
private const val WEEK = 7L

/** Суток в месяце журнала: ровно четыре недели плюс два дня. */
private const val MONTH = 30L
