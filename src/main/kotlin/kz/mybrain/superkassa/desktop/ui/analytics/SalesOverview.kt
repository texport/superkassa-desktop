package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.SalesFilter
import kz.mybrain.superkassa.desktop.server.cabinet.SalesPayments
import kz.mybrain.superkassa.desktop.server.cabinet.SalesSummary
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.server.cabinet.orZero
import kz.mybrain.superkassa.desktop.ui.history.JournalRange
import java.math.BigDecimal
import java.math.RoundingMode

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
    val revenue: BigDecimal,
    val receiptCount: Int,
    val average: BigDecimal,
    val tax: BigDecimal,
    val cashless: Int?,
    val revenueChange: Int?,
    val receiptsChange: Int?,
    val averageChange: Int?,
    val taxChange: Int?,
    val cashlessChange: Int?
)

/** Итоги срока рядом с прошлым сроком; прошлого нет — только сами числа. */
fun overviewOf(current: SalesSummary, previous: SalesSummary? = null): SalesOverview {
    val cashless = cashlessShare(current.payments)
    val cashlessWas = previous?.let { cashlessShare(it.payments) }
    return SalesOverview(
        revenue = current.revenue.orZero(),
        receiptCount = current.receiptCount,
        average = current.average,
        tax = current.tax.orZero(),
        cashless = cashless,
        revenueChange = changeOf(current.revenue.orZero(), previous?.revenue.orZero()),
        receiptsChange = changeOf(current.receiptCount, previous?.receiptCount),
        averageChange = changeOf(current.average, previous?.average),
        taxChange = changeOf(current.tax.orZero(), previous?.tax.orZero()),
        cashlessChange = if (cashless == null || cashlessWas == null) null else cashless - cashlessWas
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
    if (total.signum() <= 0) return null
    return salesPercent(cashless, total)
}

/** Изменение суммы к прошлому сроку; прошлый пуст — изменения нет. */
fun changeOf(now: BigDecimal, before: BigDecimal?): Int? {
    if (before == null || before.signum() <= 0) return null
    return salesPercent(now - before, before)
}

/** То же для счётного числа: чеков, касс, смен. */
fun changeOf(now: Int, before: Int?): Int? =
    changeOf(now.toBigDecimal(), before?.toBigDecimal())

/**
 * Касс, пробивших за срок хоть один чек.
 *
 * Своей ручки «касса на связи» у кабинета нет, и связь считается по
 * документам: касса, от которой за срок пришёл чек, до сервиса дошла.
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

/** Тот же отбор за предыдущий срок такой же длины. */
fun previousFilter(filter: SalesFilter): SalesFilter =
    JournalRange(filter.from, filter.to).shiftedBy(-1)
        .let { filter.copy(from = it.from, to = it.to) }

/** Доля целыми процентами: дробные проценты в таких числах не читают. */
internal fun salesPercent(part: BigDecimal, whole: BigDecimal): Int =
    part.multiply(WHOLE).divide(whole, 0, RoundingMode.HALF_UP).toInt()

/** Целое в процентах. */
private val WHOLE = BigDecimal.valueOf(100)
