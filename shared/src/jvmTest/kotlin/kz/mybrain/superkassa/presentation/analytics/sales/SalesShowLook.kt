package kz.mybrain.superkassa.presentation.analytics.sales

import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kz.mybrain.superkassa.domain.analytics.model.PlaceAddress
import kz.mybrain.superkassa.domain.analytics.model.SalesDay
import kz.mybrain.superkassa.domain.analytics.model.SalesDelivery
import kz.mybrain.superkassa.domain.analytics.model.SalesDeliveryCounts
import kz.mybrain.superkassa.domain.analytics.model.SalesHour
import kz.mybrain.superkassa.domain.analytics.model.SalesPayments
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.model.SalesUnit
import kz.mybrain.superkassa.domain.analytics.model.SalesView

/**
 * Сеть показа: полтора месяца по всей сети так, как их отдаёт кабинет
 * показа. Отдельно от [SalesLook]: его сводки собраны руками, эта —
 * из настоящих чисел.
 */
internal object SalesShowLook {

    private fun money(value: String): Long = SalesLook.money(value)

    /**
     * Сеть показа так, как её отдаёт кабинет.
     *
     * Полтора месяца по всей сети: 81 чек и 61 820 ₸ на одной точке
     * из тысячи, НДС нулевой — касса работает без НДС, — и три тысячи
     * касс в парке против пяти, приславших за срок хоть один чек.
     * Числа взяты из ответов кабинета показа, а не придуманы: экран,
     * который на них выглядит сломанным, сломанным его и увидят.
     */
    fun show(): SalesView = SalesView(
        range = SalesSpan(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-09-22")),
        summary = showSummary(),
        days = showDays(),
        hours = showHours(),
        registers = showRegisters(),
        places = listOf(sellingPlace()) + (1..SHOW_PLACES).map { at ->
            SalesUnit(id = "p$at", name = "Точка $at", retailPlaceName = "Точка $at")
        },
        retailPlaces = showCatalogue(),
        delivery = SalesDelivery(
            receipts = SalesDeliveryCounts(total = 111, unknown = 111),
            reports = SalesDeliveryCounts(total = 25, unknown = 25),
            offlineCount = 1
        )
    )

    /**
     * Справочник точек: сводка читает его сама — без него весь свод
     * сходился в одну строку «Без адреса» со ста процентами сети.
     */
    private fun showCatalogue(): List<PlaceAddress> = (0..SHOW_PLACES).map { at ->
        val region = SHOW_REGIONS[at % SHOW_REGIONS.size]
        PlaceAddress(
            id = if (at == 0) "p0" else "p$at",
            name = if (at == 0) "Магазин на Достык" else "Точка $at",
            address = if (at == 0) "Алматы, Медеуский, Достык, 10" else "$region, Центральный, Абая, $at"
        )
    }

    /** Единственная точка с продажами за срок. */
    private fun sellingPlace() = SalesUnit(
        id = "p0",
        name = "Магазин на Достык",
        retailPlaceName = "Магазин на Достык",
        receiptCount = SHOW_RECEIPTS,
        revenue = money("61820.00"),
        difference = money("54616.00"),
        lastContactAt = "2026-09-22T06:46:53Z"
    )

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
                SalesLook.unit(at + 1).copy(
                    receiptCount = receipts,
                    revenue = money("$sum.00"),
                    difference = money("$sum.00"),
                    retailPlaceName = "Магазин на Достык"
                )
            }
        val silent = (selling.size + 1..SHOW_FLEET).map { at ->
            SalesLook.unit(at).copy(
                receiptCount = 0,
                revenue = money("0.00"),
                difference = money("0.00"),
                lastContactAt = null
            )
        }
        return selling + silent
    }

    /** Области кабинета показа — так, как их пишет адресный регистр. */
    private val SHOW_REGIONS = listOf(
        "Алматы",
        "Астана",
        "Шымкент",
        "Мангистауская",
        "Карагандинская",
        "Ұлытау",
        "Павлодарская"
    )

    /** Парк, точки и чеки кабинета показа. */
    const val SHOW_FLEET = 3294
    const val SHOW_PLACES = 1001
    const val SHOW_RECEIPTS = 81
}
