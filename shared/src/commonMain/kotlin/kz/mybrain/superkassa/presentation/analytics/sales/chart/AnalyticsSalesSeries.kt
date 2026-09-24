package kz.mybrain.superkassa.presentation.analytics.sales.chart

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kz.mybrain.superkassa.domain.analytics.model.SalesDay
import kz.mybrain.superkassa.domain.analytics.model.SalesHour
import kz.mybrain.superkassa.domain.analytics.model.SalesPayments
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.orZero
import kz.mybrain.superkassa.domain.analytics.model.percentOf
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.format.Times
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsSalesTexts
import kz.mybrain.superkassa.strings.api.common.EnumStrings

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
data class SalesBar(val label: String, val caption: String, val value: Long)

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
fun dayBars(days: List<SalesDay>, range: SalesSpan, texts: AnalyticsSalesTexts): List<SalesBar> {
    val byDate = days.associateBy { it.date }
    return daysOf(range).map { date ->
        val found = byDate[date.toString()]
        SalesBar(
            label = dayLabel(date.toString()),
            caption = "${dayTitle(date.toString())} · ${texts.receipts}: ${Money.count(found?.receiptCount ?: 0)} · " +
                Money.formatTiyn(found?.revenue),
            value = found?.revenue.orZero()
        )
    }
}

/** Все сутки срока подряд, включая те, в которые не продали ничего. */
private fun daysOf(range: SalesSpan): List<LocalDate> =
    generateSequence(range.from) { it.plus(1, DateTimeUnit.DAY) }
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
            caption = "${Times.hour(hour)} · ${texts.receipts}: ${Money.count(found?.receiptCount ?: 0)} · " +
                Money.formatTiyn(found?.revenue),
            value = found?.revenue.orZero()
        )
    }
}

/** Доля одного вида расчёта в выручке срока. */
data class SalesShare(val title: String, val amount: Long, val percent: Int)

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
    ).map { (title, amount) -> title to amount.orZero() }.filter { it.second > 0 }
    val total = parts.sumOf { it.second }
    return parts
        .map { (title, amount) -> SalesShare(title, amount, percentOf(amount, total)) }
        .sortedByDescending { it.amount }
}

/** Подпись оси: день и месяц, год под тридцатью столбиками не помещается. */
private fun dayLabel(date: String): String =
    parsed(date)?.let { Dates.dayMonth(it) } ?: date

/** То же полной датой: её читают при наведении, и год в ней нужен. */
private fun dayTitle(date: String): String =
    parsed(date)?.let { Dates.day(it) } ?: date

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
