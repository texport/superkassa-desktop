package kz.mybrain.superkassa.domain.analytics.model

/**
 * Счёт итогов для руководства.
 *
 * Отделён от показа: изменение к прошлому сроку и доля безналичных —
 * это арифметика, и проверяется она счётом, без единой отрисовки.
 * Ошибка здесь показывает падение там, где был рост.
 */

/**
 * Главные числа срока и то, как они изменились.
 *
 * Изменение — целые проценты к прошлому сроку такой же длины; у доли
 * безналичных — процентные пункты, потому что процент от процента
 * читается как другая величина. Прошлого срока нет или он пуст —
 * изменения нет вовсе: «рост на бесконечность» не число.
 */
data class SalesOverview(
    val revenue: Long,
    val receiptCount: Int,
    val average: Long,
    val tax: Long,
    val cashless: Int?,
    /** Выручка за вычетом возвратов: ею мерят сделанное, а не пробитое. */
    val net: Long,
    val revenueChange: Int?,
    val receiptsChange: Int?,
    val averageChange: Int?,
    val taxChange: Int?,
    val cashlessChange: Int?,
    val netChange: Int?,
    /**
     * Срок ещё идёт, и сравнения нет вовсе: сегодняшний день неполон,
     * и половина сегодня против целого вчера всегда показывала падение.
     */
    val running: Boolean = false
) {
    /**
     * Начислялся ли за срок НДС.
     *
     * Нуль налога — не ошибка счёта: касса не плательщик НДС или продавала
     * без него. Голое «0,00 ₸» под словом «НДС» читалось как сбой, и о
     * причине экран обязан сказать сам.
     */
    val taxCharged: Boolean get() = tax != 0L
}

/**
 * Итоги срока рядом с прошлым сроком; прошлого нет — только сами числа.
 *
 * @param running срок ещё идёт: прошлого срока для сравнения нет намеренно.
 */
fun overviewOf(current: SalesSummary, previous: SalesSummary? = null, running: Boolean = false): SalesOverview {
    val cashless = cashlessShare(current.payments)
    val cashlessWas = previous?.let { cashlessShare(it.payments) }
    return SalesOverview(
        revenue = current.revenue.orZero(),
        receiptCount = current.receiptCount,
        average = current.average,
        tax = current.tax.orZero(),
        cashless = cashless,
        net = current.net,
        revenueChange = changeOf(current.revenue.orZero(), previous?.revenue.orZero()),
        receiptsChange = changeOf(current.receiptCount, previous?.receiptCount),
        averageChange = changeOf(current.average, previous?.average),
        taxChange = changeOf(current.tax.orZero(), previous?.tax.orZero()),
        cashlessChange = if (cashless == null || cashlessWas == null) null else cashless - cashlessWas,
        netChange = changeOf(current.net, previous?.net),
        running = running
    )
}

/**
 * Доля безналичных расчётов в процентах.
 *
 * Безналичное — карта, электронные деньги и мобильный платёж: ими
 * государство и мерит вытеснение наличных. Кредит и тара в числитель
 * не идут — это не расчёт средством платежа, а отсрочка и зачёт.
 *
 * Расчётов за срок не было вовсе — доли нет: ноль процентов безналичных
 * означал бы, что платили наличными, а платить было нечем.
 */
fun cashlessShare(payments: SalesPayments): Int? {
    val cashless = payments.card.orZero() + payments.electronic.orZero() + payments.mobile.orZero()
    val total = cashless + payments.cash.orZero() + payments.credit.orZero() +
        payments.tare.orZero() + payments.other.orZero()
    if (total <= 0) return null
    return percentOf(cashless, total)
}

/** Изменение суммы к прошлому сроку; прошлый пуст — изменения нет. */
fun changeOf(now: Long, before: Long?): Int? {
    if (before == null || before <= 0) return null
    return percentOf(now - before, before)
}

/** То же для счётного числа: чеков, касс, смен. */
fun changeOf(now: Int, before: Int?): Int? =
    changeOf(now.toLong(), before?.toLong())

/**
 * Касс, пробивших за срок хоть один чек.
 *
 * Это кассы с продажами, а не кассы на связи: связь без чеков — смена
 * без покупателей или сверка — сюда не попадает, и называть число
 * «на связи» значило обещать то, чего сводка не знает.
 */
fun sellingRegisters(registers: List<SalesUnit>): Int = registers.count { it.receiptCount > 0 }

/**
 * Касс, не пробивших за срок ни одного чека.
 *
 * Считается от числа касс компании, а не от длины списка: касса, ни разу
 * не торговавшая за срок, в ответе по кассам может не появиться вовсе,
 * и молчащей она от этого быть не перестаёт.
 */
fun silentRegisters(summary: SalesSummary, registers: List<SalesUnit>): Int {
    val known = maxOf(summary.cashRegisterCount, registers.size)
    return (known - sellingRegisters(registers)).coerceAtLeast(0)
}
