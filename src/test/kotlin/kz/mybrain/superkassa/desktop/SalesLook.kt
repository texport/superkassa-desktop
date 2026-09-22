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

    /**
     * Сеть показа так, как её отдаёт кабинет.
     *
     * Полтора месяца по всей сети: 81 чек и 61 820 ₸ на одной точке
     * из тысячи, НДС нулевой — касса работает без НДС, — и три тысячи
     * касс в парке против пяти, приславших за срок хоть один чек.
     * Числа взяты из ответов кабинета показа, а не придуманы: экран,
     * который на них выглядит сломанным, сломанным его и увидят.
     */
    fun show(): SalesView {
        val from = LocalDate.parse("2026-08-01")
        val to = LocalDate.parse("2026-09-22")
        val places = (1..SHOW_PLACES).map { at ->
            SalesUnit(id = "p$at", name = "Точка $at", retailPlaceName = "Точка $at")
        }
        val sellingPlace = SalesUnit(
            id = "p0",
            name = "Магазин на Достык",
            retailPlaceName = "Магазин на Достык",
            receiptCount = SHOW_RECEIPTS,
            revenue = money("61820.00"),
            difference = money("54616.00"),
            lastContactAt = "2026-09-22T06:46:53Z"
        )
        return SalesView(
            range = JournalRange(from, to),
            summary = showSummary(),
            days = showDays(),
            hours = showHours(),
            registers = showRegisters(),
            places = listOf(sellingPlace) + places,
            delivery = SalesDelivery(
                receipts = SalesDeliveryCounts(total = 111, unknown = 111),
                reports = SalesDeliveryCounts(total = 25, unknown = 25),
                offlineCount = 1
            )
        )
    }

    private fun showSummary() = SalesSummary(
        receiptCount = SHOW_RECEIPTS,
        revenue = money("61820.00"),
        refunds = money("7204.00"),
        difference = money("54616.00"),
        averageReceipt = money("763.21"),
        tax = money("0.00"),
        payments = SalesPayments(
            cash = money("54143.00"),
            card = money("6877.00"),
            electronic = money("0.00"),
            mobile = money("800.00")
        ),
        offlineCount = 1,
        queuedCount = 0,
        unknownCount = 111,
        cashRegisterCount = SHOW_FLEET,
        openShiftCount = 1,
        purchaseCount = 11,
        purchases = money("8420.00"),
        purchaseRefunds = money("5420.00")
    )

    /** Кабинет присылает только те сутки, в которые торговали: их пять из пятидесяти трёх. */
    private fun showDays() = listOf(
        SalesDay("2026-09-18", 32, money("31599.00"), money("404.00")),
        SalesDay("2026-09-19", 5, money("1450.00"), money("500.00")),
        SalesDay("2026-09-20", 24, money("10670.00"), money("3500.00")),
        SalesDay("2026-09-21", 4, money("2901.00"), money("2500.00")),
        SalesDay("2026-09-22", 16, money("15200.00"), money("300.00"))
    )

    private fun showHours() = listOf(
        SalesHour(21, 24, money("27293.00")),
        SalesHour(22, 9, money("6006.00")),
        SalesHour(11, 3, money("7550.00")),
        SalesHour(10, 6, money("3850.00"))
    )

    /** Пять касс с чеками и весь остальной парк молча: так отвечает кабинет. */
    private fun showRegisters(): List<SalesUnit> {
        val selling = listOf(31599 to 32, 12120 to 29, 10670 to 4, 4530 to 9, 2901 to 7)
            .mapIndexed { at, (sum, receipts) ->
                unit(at + 1).copy(
                    receiptCount = receipts,
                    revenue = money("$sum.00"),
                    difference = money("$sum.00"),
                    retailPlaceName = "Магазин на Достык"
                )
            }
        val silent = (selling.size + 1..SHOW_FLEET).map { at ->
            unit(at).copy(receiptCount = 0, revenue = money("0.00"), difference = money("0.00"), lastContactAt = null)
        }
        return selling + silent
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

    /** Парк, точки и чеки кабинета показа. */
    const val SHOW_FLEET = 3294
    const val SHOW_PLACES = 1001
    const val SHOW_RECEIPTS = 81

    const val WEEK = 7
    const val MONTH = 30
    const val HOURS = 24
    const val ROWS = 12
    const val HUNDRED = 100

    private const val RECEIPTS = 17
    private const val DAY_REVENUE = 1_284_560
    private const val HOUR_REVENUE = 53_400
}
