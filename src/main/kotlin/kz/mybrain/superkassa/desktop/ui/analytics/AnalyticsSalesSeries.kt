package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.SalesDay
import kz.mybrain.superkassa.desktop.server.cabinet.SalesHour
import kz.mybrain.superkassa.desktop.server.cabinet.SalesPayments
import kz.mybrain.superkassa.desktop.server.cabinet.orZero
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.history.JOURNAL_DAY
import kz.mybrain.superkassa.desktop.ui.history.JournalRange
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ряд столбиков для графика.
 *
 * Тип один на оба графика сводки — выручку по дням и нагрузку по часам:
 * рисуются они одинаково, а различаются только тем, что написано под
 * столбиком и что показывается при наведении на него.
 *
 * @param label подпись оси под столбиком: дата или час.
 * @param caption что читается при наведении: срок, чеки и сумма целиком.
 * @param value высота столбика; отрицательного столбика не бывает.
 */
data class SalesBar(val label: String, val caption: String, val value: BigDecimal)

/**
 * Выручка по суткам.
 *
 * Делений столько, сколько суток в сроке, — даже если кабинет прислал
 * только те, в которые торговали. За неделю с продажами в три дня он
 * отдаёт три записи, и график рисовал неделю тремя столбиками подряд:
 * провала в ней не видно вовсе, а расстояние между столбиками врёт.
 * Пустые сутки достраиваются нулём на своих местах — ровно так же, как
 * это давно делает ряд по часам.
 *
 * Порядок — от первых суток срока к последним: ось времени идёт слева
 * направо, и сортировать её заново значило бы спорить с тем, что
 * владелец видит на всех остальных графиках.
 */
fun dayBars(days: List<SalesDay>, range: JournalRange, texts: AnalyticsSalesTexts): List<SalesBar> {
    val byDate = days.associateBy { it.date }
    return daysOf(range).map { date ->
        val found = byDate[date.toString()]
        SalesBar(
            label = dayLabel(date.toString()),
            caption = "${dayTitle(date.toString())} · ${texts.receipts}: ${found?.receiptCount ?: 0} · " +
                cabinetSum(found?.revenue),
            value = found?.revenue.orZero()
        )
    }
}

/** Все сутки срока подряд, включая те, в которые не продали ничего. */
private fun daysOf(range: JournalRange): List<LocalDate> =
    generateSequence(range.from) { it.plusDays(1) }
        .takeWhile { it <= range.to }
        .toList()

/**
 * Нагрузка по часам суток.
 *
 * Делений ровно двадцать четыре, даже если кабинет прислал только часы
 * с продажами: провал в середине дня виден только тогда, когда пустые
 * часы стоят на своих местах, а не выброшены из ряда.
 */
fun hourBars(hours: List<SalesHour>, texts: AnalyticsSalesTexts): List<SalesBar> {
    val byHour = hours.associateBy { it.hour }
    return (0 until DAY_HOURS).map { hour ->
        val found = byHour[hour]
        SalesBar(
            label = hour.toString(),
            caption = "${hourTitle(hour)} · ${texts.receipts}: ${found?.receiptCount ?: 0} · " +
                cabinetSum(found?.revenue),
            value = found?.revenue.orZero()
        )
    }
}

/** Доля одного вида расчёта в выручке срока. */
data class SalesShare(val title: String, val amount: BigDecimal, val percent: Int)

/**
 * Виды расчётов долями от целого.
 *
 * Вид, которым за срок не заплатили ни разу, в набор не попадает вовсе:
 * пустая доля рисовалась бы полосой нулевой ширины со своей подписью
 * и своим цветом — шесть таких подписей под однотонной полосой.
 *
 * Крупные виды идут первыми: владелец смотрит на этот раздел, чтобы
 * увидеть, чем платят чаще, и порядок ответом на этот вопрос и является.
 *
 * Называются виды расчётов словами справочника кассы, а не своими:
 * на чеке напечатано то же самое.
 */
fun salesShares(payments: SalesPayments, enums: EnumStrings, other: String): List<SalesShare> {
    val parts = listOf(
        enums.paymentCash to payments.cash,
        enums.paymentCard to payments.card,
        enums.paymentMobile to payments.mobile,
        enums.paymentCredit to payments.credit,
        enums.paymentTare to payments.tare,
        other to payments.other
    ).map { (title, amount) -> title to amount.orZero() }.filter { it.second.signum() > 0 }
    val total = parts.fold(BigDecimal.ZERO) { sum, part -> sum + part.second }
    return parts
        .map { (title, amount) -> SalesShare(title, amount, percentOf(amount, total)) }
        .sortedByDescending { it.amount }
}

/** Доля в процентах целым числом: дробные проценты под полосой не читают. */
private fun percentOf(amount: BigDecimal, total: BigDecimal): Int =
    if (total.signum() <= 0) 0 else amount.multiply(HUNDRED).divide(total, 0, RoundingMode.HALF_UP).toInt()

/** Целое в процентах. */
private val HUNDRED = BigDecimal.valueOf(100)

/** Подпись оси: день и месяц, год под тридцатью столбиками не помещается. */
private fun dayLabel(date: String): String =
    parsed(date)?.let { DAY_LABEL.format(it) } ?: date

/** То же полной датой: её читают при наведении, и год в ней нужен. */
private fun dayTitle(date: String): String =
    parsed(date)?.let { JOURNAL_DAY.format(it) } ?: date

/** Час целиком: «14:00». */
private fun hourTitle(hour: Int): String = "%02d:00".format(hour)

/**
 * Дата суток из ответа кабинета.
 *
 * Кабинет отдаёт её строкой `2026-09-19`. Не разобралась — показывается
 * как пришла: неизвестный вид даты не повод оставить столбик без подписи.
 */
private fun parsed(date: String): LocalDate? =
    date.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Часов в сутках: делений у графика нагрузки. */
private const val DAY_HOURS = 24

private val DAY_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")
