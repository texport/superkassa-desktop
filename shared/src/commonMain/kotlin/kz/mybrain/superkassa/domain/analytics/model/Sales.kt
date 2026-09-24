package kz.mybrain.superkassa.domain.analytics.model

/**
 * Торговая сводка кабинета: что продано за срок и как это доехало до ОФД.
 *
 * Считает кабинет, а не приложение: чеки лежат у него, и пересчитывать их
 * на рабочем месте значило бы выкачивать месяц документов ради пяти чисел.
 *
 * Каждое поле имеет значение по умолчанию: сторону кабинета пишут прямо
 * сейчас, и недостающее число показывается прочерком — экран не падает
 * и не выдумывает данные. Деньги — целые тиыны, как и у ядра кассы.
 */

/**
 * Отбор сводки.
 *
 * Срок обязателен всем ручкам: сводка без границ — это вся история
 * компании, и кабинет такого не считает. Точка и касса необязательны:
 * без них сводка идёт по всей компании.
 */
data class SalesFilter(
    /** Первые сутки срока, `ГГГГ-ММ-ДД`: так их пишет и кабинет. */
    val from: String,
    /** Последние сутки срока, включительно. */
    val to: String,
    val retailPlaceId: String? = null,
    val cashRegisterId: String? = null
)

/** Суммы по видам расчётов за срок. */
data class SalesPayments(
    val cash: Long? = null,
    val card: Long? = null,
    val electronic: Long? = null,
    val mobile: Long? = null,
    val credit: Long? = null,
    val tare: Long? = null,
    val other: Long? = null
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
data class SalesSummary(
    val receiptCount: Int = 0,
    val revenue: Long? = null,
    val refunds: Long? = null,
    val difference: Long? = null,
    val averageReceipt: Long? = null,
    val tax: Long? = null,
    val payments: SalesPayments = SalesPayments(),
    val offlineCount: Int = 0,
    val queuedCount: Int = 0,
    val unknownCount: Int = 0,
    val cashRegisterCount: Int = 0,
    val openShiftCount: Int = 0,
    val purchaseCount: Int = 0,
    val purchases: Long? = null,
    val purchaseRefunds: Long? = null
) {

    /**
     * Было ли за срок хоть что-то куплено у населения: ряд нулей
     * у кассы, которая ничего не скупает, о сроке не говорит ничего.
     *
     * Считается по числам, а не по тому, пришло ли поле: кабинет шлёт
     * `"purchases": 0.00` и за срок без единой покупки, и пустой срок
     * из-за этого выглядел сроком с покупками — шестью плитками нулей
     * вместо слов о том, что документов нет.
     */
    val purchased: Boolean
        get() = purchaseCount > 0 || purchases.orZero() != 0L || purchaseRefunds.orZero() != 0L

    /** Выручка за вычетом возвратов. */
    val net: Long get() = difference ?: (revenue.orZero() - refunds.orZero())

    /** Средний чек: своего нет — делится выручка на число чеков, до тиына. */
    val average: Long
        get() = averageReceipt ?: when {
            receiptCount <= 0 -> 0
            else -> halfUpDiv(revenue.orZero(), receiptCount.toLong())
        }
}

/** Сутки срока: по ним рисуются столбики выручки. */
data class SalesDay(
    val date: String = "",
    val receiptCount: Int = 0,
    val revenue: Long? = null,
    val refunds: Long? = null,
    val difference: Long? = null
)

/** Час суток: 0..23, сложенные по всем дням срока. */
data class SalesHour(
    val hour: Int = 0,
    val receiptCount: Int = 0,
    val revenue: Long? = null
)

/**
 * Строка сводки по кассе или по торговой точке.
 *
 * Тип один на обе таблицы: считают в них одно и то же — чеки, выручку
 * и разность, — а различаются только заголовком строки. Вторая копия
 * с теми же пятью числами разошлась бы с первой на первой же правке.
 */
data class SalesUnit(
    val id: String? = null,
    val registrationNumber: String? = null,
    val name: String? = null,
    val retailPlaceName: String? = null,
    val receiptCount: Int = 0,
    val revenue: Long? = null,
    val difference: Long? = null,
    val lastContactAt: String? = null
)

/**
 * Состояние доставки одного вида документов.
 *
 * Кабинет считает чеки и отчёты порознь: у них разные сроки хранения
 * и разная цена потери, и сводить их в одно число на его стороне значило
 * бы лишать владельца возможности различить.
 */
data class SalesDeliveryCounts(
    val total: Int = 0,
    val delivered: Int = 0,
    val queued: Int = 0,
    /**
     * О доставке документа не известно ничего.
     *
     * Это не очередь: служба передачи в КГД может быть не развёрнута
     * вовсе, и тогда о судьбе документа за пределами сервиса приёма
     * кабинет не знает. Называть такое очередью значит обещать владельцу
     * ответ, которого никто не ждёт.
     */
    val unknown: Int = 0,
    val rejected: Int = 0
)

/**
 * Состояние доставки документов за срок.
 *
 * Отбракованное — единственное, что требует работы владельца:
 * доставленное и стоящее в очереди доедут сами. На экране виды
 * документов складываются: владельцу важно, что не доехало, а не
 * какого оно вида — вид виден в журнале.
 */
data class SalesDelivery(
    val receipts: SalesDeliveryCounts = SalesDeliveryCounts(),
    val reports: SalesDeliveryCounts = SalesDeliveryCounts(),
    val offlineCount: Int = 0
) {
    val delivered: Int get() = receipts.delivered + reports.delivered
    val queued: Int get() = receipts.queued + reports.queued
    val unknown: Int get() = receipts.unknown + reports.unknown
    val rejected: Int get() = receipts.rejected + reports.rejected
    val offline: Int get() = offlineCount
}

/**
 * Разрезы торговой сводки одного срока.
 *
 * Отбор у всех один, поэтому и лежат они вместе: разные сроки у соседних
 * чисел на одном экране означали бы, что они говорят о разном.
 */
data class SalesFigures(
    val summary: SalesSummary,
    val days: List<SalesDay>,
    val hours: List<SalesHour>,
    val registers: List<SalesUnit>,
    val places: List<SalesUnit>,
    val delivery: SalesDelivery
)
