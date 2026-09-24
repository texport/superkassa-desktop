@file:OptIn(ExperimentalSerializationApi::class)

package kz.mybrain.superkassa.integrations.bfdcabinet.analytics

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/**
 * Отбор торговой сводки. Срок обязателен: сводку без границ кабинет не считает.
 *
 * @property from первые сутки срока, `ГГГГ-ММ-ДД`.
 * @property to последние сутки срока, включительно.
 * @property retailPlaceId только эта точка; `null` — вся компания.
 * @property cashRegisterId только эта касса; `null` — все кассы.
 */
data class SalesFilter(
    val from: String,
    val to: String,
    val retailPlaceId: String? = null,
    val cashRegisterId: String? = null
)

/** Суммы по видам расчётов за срок, в тенге. */
@Serializable
data class SalesPayments(
    val cash: CabinetDecimal? = null,
    val card: CabinetDecimal? = null,
    val electronic: CabinetDecimal? = null,
    @JsonNames("mobileMoney") val mobile: CabinetDecimal? = null,
    val credit: CabinetDecimal? = null,
    val tare: CabinetDecimal? = null,
    @JsonNames("rest") val other: CabinetDecimal? = null
)

/**
 * Главные числа срока, как их посчитал кабинет.
 *
 * Сторону кабинета пишут сейчас: у спорных полей перечислены возможные имена,
 * у каждого есть значение по умолчанию. Разность и средний чек кабинет
 * может не прислать — тогда их выводит приложение. Покупка у населения —
 * отдельными числами: ни в выручку, ни в возвраты она не входит.
 */
@Serializable
data class SalesSummary(
    @JsonNames("receipts", "ticketCount") val receiptCount: Int = 0,
    @JsonNames("total", "salesSum") val revenue: CabinetDecimal? = null,
    @JsonNames("returns", "returnsSum") val refunds: CabinetDecimal? = null,
    @JsonNames("net", "netRevenue") val difference: CabinetDecimal? = null,
    @JsonNames("average", "averageCheck") val averageReceipt: CabinetDecimal? = null,
    @JsonNames("taxTotal", "taxSum") val tax: CabinetDecimal? = null,
    @JsonNames("paymentTypes") val payments: SalesPayments = SalesPayments(),
    @JsonNames("offlineDocuments", "autonomous") val offlineCount: Int = 0,
    @JsonNames("queuedDocuments") val queuedCount: Int = 0,
    @JsonNames("unknownDocuments", "undeliveredCount") val unknownCount: Int = 0,
    @JsonNames("cashRegisters") val cashRegisterCount: Int = 0,
    @JsonNames("openShifts") val openShiftCount: Int = 0,
    @JsonNames("buyCount") val purchaseCount: Int = 0,
    @JsonNames("buys") val purchases: CabinetDecimal? = null,
    @JsonNames("buyRefunds") val purchaseRefunds: CabinetDecimal? = null
)

/** Сутки срока. */
@Serializable
data class SalesDay(
    @JsonNames("day") val date: String = "",
    @JsonNames("receipts") val receiptCount: Int = 0,
    @JsonNames("total") val revenue: CabinetDecimal? = null,
    @JsonNames("returns") val refunds: CabinetDecimal? = null,
    @JsonNames("net", "netRevenue") val difference: CabinetDecimal? = null
)

/** Час суток 0..23, сложенный по всем дням срока. */
@Serializable
data class SalesHour(
    val hour: Int = 0,
    @JsonNames("receipts") val receiptCount: Int = 0,
    @JsonNames("total") val revenue: CabinetDecimal? = null
)

/** Строка сводки по кассе или по торговой точке — одна форма на обе таблицы. */
@Serializable
data class SalesUnit(
    @JsonNames("cashRegisterId", "retailPlaceId") val id: String? = null,
    @JsonNames("rnm") val registrationNumber: String? = null,
    @JsonNames("internalName", "title") val name: String? = null,
    @JsonNames("retailPlace") val retailPlaceName: String? = null,
    @JsonNames("receipts") val receiptCount: Int = 0,
    @JsonNames("total") val revenue: CabinetDecimal? = null,
    @JsonNames("net", "netRevenue") val difference: CabinetDecimal? = null,
    @JsonNames("lastSeen") val lastContactAt: String? = null
)

/**
 * Доставка одного вида документов.
 *
 * @property unknown о доставке не известно ничего: служба передачи в КГД
 *   может быть не развёрнута — это не очередь.
 */
@Serializable
data class SalesDeliveryCounts(
    val total: Int = 0,
    val delivered: Int = 0,
    @JsonNames("inQueue", "pending") val queued: Int = 0,
    val unknown: Int = 0,
    @JsonNames("refused") val rejected: Int = 0
)

/** Доставка документов за срок: чеки и отчёты порознь. */
@Serializable
data class SalesDelivery(
    val receipts: SalesDeliveryCounts = SalesDeliveryCounts(),
    val reports: SalesDeliveryCounts = SalesDeliveryCounts(),
    @JsonNames("autonomous", "offline") val offlineCount: Int = 0
)

/** Все разрезы сводки одного срока: отбор у них один. */
data class SalesFigures(
    val summary: SalesSummary,
    val days: List<SalesDay>,
    val hours: List<SalesHour>,
    val registers: List<SalesUnit>,
    val places: List<SalesUnit>,
    val delivery: SalesDelivery
)
