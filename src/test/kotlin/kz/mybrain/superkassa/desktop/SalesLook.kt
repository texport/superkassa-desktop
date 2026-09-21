package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.SalesDay
import kz.mybrain.superkassa.desktop.server.cabinet.SalesDelivery
import kz.mybrain.superkassa.desktop.server.cabinet.SalesDeliveryCounts
import kz.mybrain.superkassa.desktop.server.cabinet.SalesHour
import kz.mybrain.superkassa.desktop.server.cabinet.SalesPayments
import kz.mybrain.superkassa.desktop.server.cabinet.SalesSummary
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.ui.analytics.SalesView
import kz.mybrain.superkassa.desktop.ui.history.JournalRange
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Составы торговой сводки для снимков.
 *
 * Собраны отдельно от самих снимков: один и тот же срок нужен и целой
 * сводке, и одной таблице, и пустому дню, а собранный в каждом снимке
 * заново он расходился бы числами — и разница на картинке была бы не та,
 * которую смотрят.
 */
internal object SalesLook {

    val firstDay: LocalDate = LocalDate.parse("2026-09-01")

    fun money(value: String): BigDecimal = BigDecimal(value)

    fun day(at: Int) = SalesDay(
        date = "2026-09-%02d".format(at),
        receiptCount = at * RECEIPTS,
        revenue = money("${at * DAY_REVENUE}.00"),
        refunds = money("0.00")
    )

    fun unit(at: Int) = SalesUnit(
        id = "c$at",
        registrationNumber = "%012d".format(4500000L + at),
        name = "Касса $at",
        retailPlaceName = "Магазин $at",
        receiptCount = at * RECEIPTS,
        revenue = money("${at * DAY_REVENUE}.00"),
        difference = money("${at * DAY_REVENUE - at}.00"),
        lastContactAt = "2026-09-19T08:14:00Z"
    )

    /** Обычный срок: неделя торговли сети из десятка касс. */
    fun view(days: Int = WEEK, rows: Int = ROWS) = SalesView(
        range = JournalRange(firstDay, firstDay.plusDays(days - 1L)),
        summary = summary(rows),
        days = (1..days).map(::day),
        hours = (0 until HOURS).map { SalesHour(it, it * RECEIPTS, money("${it * HOUR_REVENUE}.00")) },
        registers = (1..rows).map(::unit),
        places = (1..rows).map(::unit),
        delivery = SalesDelivery(
            receipts = SalesDeliveryCounts(total = 3128, delivered = 3000, queued = 105, unknown = 21, rejected = 2),
            offlineCount = 7
        )
    )

    /** Срок без единой продажи: нули по всем полям, но срок непустой. */
    fun nothingSold(): SalesView = view(days = 1, rows = 1).let {
        it.copy(
            summary = SalesSummary(cashRegisterCount = 1),
            days = listOf(SalesDay(date = "2026-09-01", receiptCount = 0, revenue = money("0.00"))),
            hours = emptyList(),
            registers = emptyList(),
            places = emptyList(),
            delivery = SalesDelivery(receipts = SalesDeliveryCounts(), offlineCount = 0)
        )
    }

    /** Единственный вид расчёта: доля обязана быть целой, а не долькой. */
    fun onlyCash(): SalesView = view().let {
        it.copy(summary = it.summary.copy(payments = SalesPayments(cash = it.summary.revenue)))
    }

    private fun summary(rows: Int) = SalesSummary(
        receiptCount = 12845,
        revenue = money("128456000.00"),
        refunds = money("12400.50"),
        tax = money("13763142.86"),
        payments = SalesPayments(cash = money("40000000.00"), card = money("88456000.00")),
        offlineCount = 3,
        queuedCount = 2,
        unknownCount = 5,
        cashRegisterCount = rows,
        openShiftCount = 4
    )

    const val WEEK = 7
    const val MONTH = 30
    const val HOURS = 24
    const val ROWS = 12
    const val HUNDRED = 100

    private const val RECEIPTS = 17
    private const val DAY_REVENUE = 1_284_560
    private const val HOUR_REVENUE = 53_400
}
