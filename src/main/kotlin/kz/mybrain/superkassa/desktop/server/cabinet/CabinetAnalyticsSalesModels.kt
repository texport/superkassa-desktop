@file:OptIn(ExperimentalSerializationApi::class)

package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Торговая сводка кабинета: что продано за срок и как это доехало до ОФД.
 *
 * Считает кабинет, а не приложение: чеки лежат у него, и пересчитывать их
 * на рабочем месте значило бы выкачивать месяц документов ради пяти чисел.
 *
 * Разбор нарочно снисходительный. Сторону кабинета пишут прямо сейчас,
 * и точные имена полей ещё сдвинутся: каждое поле имеет значение
 * по умолчанию, у спорных перечислены возможные имена, а незнакомые
 * ключи отбрасываются настройкой клиента. Недостающее число показывается
 * прочерком — экран не падает и не выдумывает данные.
 *
 * Деньги — десятичные тенге, как и во всём кабинете; сотая доля тенге
 * называется тиын.
 */

/**
 * Отбор сводки.
 *
 * Срок обязателен всем ручкам: сводка без границ — это вся история
 * компании, и кабинет такого не считает. Точка и касса необязательны:
 * без них сводка идёт по всей компании.
 */
data class SalesFilter(
    val from: LocalDate,
    val to: LocalDate,
    val retailPlaceId: String? = null,
    val cashRegisterId: String? = null
) {
    /** Хвост запроса; незаданный отбор не пишется вовсе. */
    fun query(): String = "?" + listOfNotNull(
        "from=$from",
        "to=$to",
        retailPlaceId?.takeIf { it.isNotBlank() }?.let { "retailPlaceId=${it.encoded()}" },
        cashRegisterId?.takeIf { it.isNotBlank() }?.let { "cashRegisterId=${it.encoded()}" }
    ).joinToString("&")
}

/** Суммы по видам расчётов за срок. */
@Serializable
data class SalesPayments(
    @Contextual val cash: BigDecimal? = null,
    @Contextual val card: BigDecimal? = null,
    @Contextual val electronic: BigDecimal? = null,
    @JsonNames("mobileMoney") @Contextual val mobile: BigDecimal? = null,
    @Contextual val credit: BigDecimal? = null,
    @Contextual val tare: BigDecimal? = null,
    @JsonNames("rest") @Contextual val other: BigDecimal? = null
)

/**
 * Главные числа срока.
 *
 * Разность и средний чек кабинет считает сам, но если не прислал —
 * они выводятся здесь из выручки, возвратов и числа чеков. Точная
 * десятичная арифметика: средний чек округляется до тиына.
 *
 * Покупка у населения стоит отдельными числами и ни в выручку, ни
 * в возвраты не входит: там касса деньги получает, здесь выдаёт. Пока
 * сторона кабинета не выложена, полей в ответе нет — и покупок за срок
 * просто не показывают.
 */
@Serializable
data class SalesSummary(
    @JsonNames("receipts", "ticketCount") val receiptCount: Int = 0,
    @JsonNames("total", "salesSum") @Contextual val revenue: BigDecimal? = null,
    @JsonNames("returns", "returnsSum") @Contextual val refunds: BigDecimal? = null,
    @JsonNames("net", "netRevenue") @Contextual val difference: BigDecimal? = null,
    @JsonNames("average", "averageCheck") @Contextual val averageReceipt: BigDecimal? = null,
    @JsonNames("taxTotal", "taxSum") @Contextual val tax: BigDecimal? = null,
    @JsonNames("paymentTypes") val payments: SalesPayments = SalesPayments(),
    @JsonNames("offlineDocuments", "autonomous") val offlineCount: Int = 0,
    @JsonNames("queuedDocuments") val queuedCount: Int = 0,
    @JsonNames("unknownDocuments", "undeliveredCount") val unknownCount: Int = 0,
    @JsonNames("cashRegisters") val cashRegisterCount: Int = 0,
    @JsonNames("openShifts") val openShiftCount: Int = 0,
    @JsonNames("buyCount") val purchaseCount: Int = 0,
    @JsonNames("buys") @Contextual val purchases: BigDecimal? = null,
    @JsonNames("buyRefunds") @Contextual val purchaseRefunds: BigDecimal? = null
) {

    /**
     * Было ли за срок хоть что-то куплено у населения: ряд нулей
     * у кассы, которая ничего не скупает, о сроке не говорит ничего.
     */
    val purchased: Boolean
        get() = purchaseCount > 0 || purchases != null || purchaseRefunds != null

    /** Выручка за вычетом возвратов. */
    val net: BigDecimal get() = difference ?: (revenue.orZero() - refunds.orZero())

    /** Средний чек: своего нет — делится выручка на число чеков. */
    val average: BigDecimal
        get() = averageReceipt ?: when {
            receiptCount <= 0 -> BigDecimal.ZERO
            else -> revenue.orZero().divide(receiptCount.toBigDecimal(), TIYN, RoundingMode.HALF_UP)
        }
}

/** Сутки срока: по ним рисуются столбики выручки. */
@Serializable
data class SalesDay(
    @JsonNames("day") val date: String = "",
    @JsonNames("receipts") val receiptCount: Int = 0,
    @JsonNames("total") @Contextual val revenue: BigDecimal? = null,
    @JsonNames("returns") @Contextual val refunds: BigDecimal? = null,
    @JsonNames("net", "netRevenue") @Contextual val difference: BigDecimal? = null
)

/** Час суток: 0..23, сложенные по всем дням срока. */
@Serializable
data class SalesHour(
    val hour: Int = 0,
    @JsonNames("receipts") val receiptCount: Int = 0,
    @JsonNames("total") @Contextual val revenue: BigDecimal? = null
)

/**
 * Строка сводки по кассе или по торговой точке.
 *
 * Тип один на обе таблицы: считают в них одно и то же — чеки, выручку
 * и разность, — а различаются только заголовком строки. Вторая копия
 * с теми же пятью числами разошлась бы с первой на первой же правке.
 */
@Serializable
data class SalesUnit(
    @JsonNames("cashRegisterId", "retailPlaceId") val id: String? = null,
    @JsonNames("rnm") val registrationNumber: String? = null,
    @JsonNames("internalName", "title") val name: String? = null,
    @JsonNames("retailPlace") val retailPlaceName: String? = null,
    @JsonNames("receipts") val receiptCount: Int = 0,
    @JsonNames("total") @Contextual val revenue: BigDecimal? = null,
    @JsonNames("net", "netRevenue") @Contextual val difference: BigDecimal? = null,
    @JsonNames("lastSeen") val lastContactAt: String? = null
)

/**
 * Состояние доставки одного вида документов.
 *
 * Кабинет считает чеки и отчёты порознь: у них разные сроки хранения
 * и разная цена потери, и сводить их в одно число на его стороне значило
 * бы лишать владельца возможности различить.
 */
@Serializable
data class SalesDeliveryCounts(
    val total: Int = 0,
    val delivered: Int = 0,
    @JsonNames("inQueue", "pending") val queued: Int = 0,
    /**
     * О доставке документа не известно ничего.
     *
     * Это не очередь: служба передачи в КГД может быть не развёрнута
     * вовсе, и тогда о судьбе документа за пределами сервиса приёма
     * кабинет не знает. Называть такое очередью значит обещать владельцу
     * ответ, которого никто не ждёт.
     */
    val unknown: Int = 0,
    @JsonNames("refused") val rejected: Int = 0
)

/**
 * Состояние доставки документов за срок.
 *
 * Отбракованное — единственное, что требует работы владельца:
 * доставленное и стоящее в очереди доедут сами. На экране виды
 * документов складываются: владельцу важно, что не доехало, а не
 * какого оно вида — вид виден в журнале.
 */
@Serializable
data class SalesDelivery(
    val receipts: SalesDeliveryCounts = SalesDeliveryCounts(),
    val reports: SalesDeliveryCounts = SalesDeliveryCounts(),
    @JsonNames("autonomous", "offline") val offlineCount: Int = 0
) {
    val delivered: Int get() = receipts.delivered + reports.delivered
    val queued: Int get() = receipts.queued + reports.queued
    val unknown: Int get() = receipts.unknown + reports.unknown
    val rejected: Int get() = receipts.rejected + reports.rejected
    val offline: Int get() = offlineCount
}

/** Незаполненная сумма считается нулём, а не прочерком в арифметике. */
internal fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

/** Знаков после запятой у тенге: сотая доля — тиын. */
private const val TIYN = 2
