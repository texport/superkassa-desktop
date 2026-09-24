package kz.mybrain.superkassa.domain.kassa.model.payment

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import kz.mybrain.superkassa.domain.kassa.model.Tenge

/**
 * Чем платят за чек — одним видом или несколькими сразу.
 *
 * Покупатель приносит часть суммы картой, часть наличными: это обычный
 * расчёт в магазине, а не редкий случай. Протокол принимает список оплат,
 * и разбиение живёт здесь — одинаково для продажи и для возврата.
 *
 * Одна оплата остаётся без ввода и берёт остаток чека. Так суммы
 * складываются в итог по построению: кассиру нечего сводить в уме, а кассе
 * не приходит чек, в котором заплачено не столько, сколько пробито.
 *
 * Разбиение неизменяемое: правка возвращает новое, и модель экрана держит
 * его в своём состоянии, а не в памяти экрана. Суммы — в тиынах.
 */
data class PaymentSplit(val entries: List<PaymentLine> = listOf(PaymentLine(CASH_PAYMENT))) {

    /** Оплат больше одной: суммы вводятся, а не подразумеваются. */
    val mixed: Boolean get() = entries.size > 1

    /** Виды оплаты, выбранные сейчас. */
    val types: List<String> get() = entries.map { it.type }

    /** Платят ли наличными хотя бы частью. */
    val hasCash: Boolean get() = entries.any { it.type == CASH_PAYMENT }

    /**
     * Номер оплаты, которая забирает остаток чека и ввода не требует.
     *
     * Это наличные, когда ими платят: смешанный расчёт затевают, когда
     * карты не хватает, и остальное покупатель добирает деньгами. Спросить
     * наличную часть значило бы просить кассира вычесть итог из карты
     * в уме — и принять его ошибку, если он вычтет не так. Без наличных
     * остаток забирает последняя оплата.
     */
    val rest: Int get() = entries.indexOfFirst { it.type == CASH_PAYMENT }.takeIf { it >= 0 } ?: entries.lastIndex

    /** Добавляет оплату указанного вида остатком чека. */
    fun add(type: String): PaymentSplit =
        if (entries.any { it.type == type }) this else copy(entries = entries + PaymentLine(type))

    /** Убирает оплату; последнюю оставшуюся убрать нельзя. */
    fun remove(at: Int): PaymentSplit =
        if (!mixed || at !in entries.indices) this else copy(entries = entries.filterIndexed { i, _ -> i != at })

    /** Меняет вид оплаты в строке, если такой ещё не выбран в другой. */
    fun retype(at: Int, type: String): PaymentSplit {
        val taken = entries.withIndex().any { (i, line) -> i != at && line.type == type }
        return if (taken || at !in entries.indices) this else edit(at) { it.copy(type = type) }
    }

    /**
     * Сумма, набранная кассиром в строке.
     *
     * Набранное хранится и у строки, которая сейчас берёт остаток: станет
     * она набираемой, когда добавят наличные, — кассир увидит своё число.
     */
    fun enter(at: Int, amount: String): PaymentSplit =
        if (at !in entries.indices) this else edit(at) { it.copy(amount = amount) }

    /** Эта ли оплата берёт остаток: её сумму касса считает сама. */
    fun takesRest(at: Int): Boolean = at == rest

    /**
     * Сумма оплаты в строке.
     *
     * Единственная оплата — весь итог: спрашивать сумму, когда она и так
     * известна, значит требовать ввод ради ввода.
     */
    fun sumOf(at: Int, total: Long): Long =
        if (takesRest(at)) total - assigned() else entries[at].value ?: 0L

    /** Оплаты для кассы. */
    fun toPayments(total: Long): List<ReceiptPaymentRequest> =
        entries.indices.map { ReceiptPaymentRequest(entries[it].type, Tenge.decimal(sumOf(it, total))) }

    /** Наличная часть чека: от неё считается сдача. */
    fun cashSum(total: Long): Long =
        entries.indices.filter { entries[it].type == CASH_PAYMENT }.sumOf { sumOf(it, total) }

    /** Что уже расписано по видам, кроме остатка. */
    fun assigned(): Long = entered().sumOf { it.value ?: 0L }

    /** Почему такое разбиение принять нельзя, или `null`. */
    fun issue(total: Long): SplitIssue? = when {
        !mixed -> null
        entered().any { (it.value ?: 0L) <= 0L } -> SplitIssue.Empty
        assigned() >= total -> SplitIssue.Excess
        else -> null
    }

    /** Возвращает разбиение к одной оплате: следующий чек начинается с чистого. */
    fun reset(): PaymentSplit = PaymentSplit(listOf(PaymentLine(entries.first().type)))

    /** Оплаты, суммы которых набирает кассир. */
    private fun entered(): List<PaymentLine> = entries.filterIndexed { at, _ -> !takesRest(at) }

    private fun edit(at: Int, change: (PaymentLine) -> PaymentLine): PaymentSplit =
        copy(entries = entries.mapIndexed { i, line -> if (i == at) change(line) else line })
}

/**
 * Одна оплата: чем и сколько.
 *
 * Сумма хранится текстом, как её набрал кассир: разобранное число теряет
 * незаконченный ввод, и поле дёргалось бы под руками.
 */
data class PaymentLine(val type: String, val amount: String = "") {

    /**
     * Введённая сумма в тиынах или `null`, если поле пустое либо набрано не суммой.
     *
     * Разбор тот же, что у всякой суммы кассы: деньги делятся до тиына
     * и не глубже. Свой разбор принимал и доли тиына, и запись вида
     * «1E3» — такая оплата уходила в ОФД, а остаток второй строки
     * считался от неё.
     */
    val value: Long? get() = Tenge.parse(amount)
}

/** Чем разбиение оплаты не годится. */
enum class SplitIssue { Empty, Excess }

/** Вид оплаты по умолчанию: наличные встречаются чаще прочих. */
internal const val CASH_PAYMENT: String = "CASH"
