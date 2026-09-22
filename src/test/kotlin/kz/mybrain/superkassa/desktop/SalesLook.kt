package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
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


    /**
     * Тот же срок, под которым лежит прошлый: сеть выросла.
     *
     * Прошлый срок отличается от нынешнего всеми числами сразу — выручкой,
     * чеками, налогом и видами расчётов: на снимке смотрят, что стрелки
     * и цвета у всех пяти чисел разошлись правильно, а не совпали случайно.
     */
    fun growing(): SalesView = view().let {
        it.copy(
            previous = it.summary.copy(
                receiptCount = 11_230,
                revenue = money("104000000.00"),
                tax = money("11142857.14"),
                payments = SalesPayments(cash = money("46000000.00"), card = money("58000000.00"))
            ),
            retailPlaces = catalogue(REGIONS)
        )
    }

    /** Тот же срок, но прошлый был лучше: числа идут под уклон. */
    fun falling(): SalesView = view().let {
        it.copy(
            previous = it.summary.copy(
                receiptCount = 15_900,
                revenue = money("162000000.00"),
                tax = money("17357142.86"),
                payments = SalesPayments(cash = money("48000000.00"), card = money("114000000.00"))
            ),
            retailPlaces = catalogue(REGIONS)
        )
    }

    /**
     * Сеть, разложенная по регионам.
     *
     * По две точки на регион и по кассе на точку: на снимке смотрят и свод
     * точек, и счёт работавших машин. Выручка убывает от региона к региону,
     * чтобы на картинке было видно, что порядок строк — по выручке.
     */
    fun regioned(count: Int = REGIONS): SalesView {
        val places = catalogue(count)
        val rows = places.mapIndexed { at, place ->
            val weight = count * 2 - at
            unit(at + 1).copy(
                id = place.id,
                name = place.name,
                receiptCount = weight * RECEIPTS,
                revenue = money("${weight * DAY_REVENUE}.00")
            )
        }
        val registers = places.mapIndexed { at, place ->
            unit(at + 1).copy(retailPlaceName = place.name, receiptCount = if (at == SILENT) 0 else at * RECEIPTS + 1)
        }
        return view().copy(places = rows, registers = registers, retailPlaces = places)
    }

    /** Справочник точек: по две на регион, с адресом кабинета «Регион, Район, Улица, Дом». */
    private fun catalogue(count: Int): List<RetailPlace> =
        REGION_NAMES.take(count).flatMapIndexed { at, region ->
            listOf("Центральный" to "Абая", "Северный" to "Сейфуллина").mapIndexed { which, (area, street) ->
                RetailPlace(
                    id = "p$at$which",
                    name = "$region, $area",
                    address = "$region, $area район, $street, ${at * 2 + which + 1}"
                )
            }
        }

    /** Регионы Казахстана, в которых стоит сеть снимка. */
    private val REGION_NAMES = listOf(
        "Алматы",
        "Астана",
        "Шымкент",
        "Карагандинская область",
        "Актюбинская область",
        "Восточно-Казахстанская область",
        "Мангистауская область"
    )

    /** Сколько регионов в сети снимка. */
    const val REGIONS = 7

    /** Которая по счёту точка снимка молчит: на ней видно, что молчащие в счёт не идут. */
    private const val SILENT = 3

    const val WEEK = 7
    const val MONTH = 30
    const val HOURS = 24
    const val ROWS = 12
    const val HUNDRED = 100

    private const val RECEIPTS = 17
    private const val DAY_REVENUE = 1_284_560
    private const val HOUR_REVENUE = 53_400
}
