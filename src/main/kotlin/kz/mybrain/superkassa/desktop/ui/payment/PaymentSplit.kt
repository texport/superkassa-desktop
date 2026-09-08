package kz.mybrain.superkassa.desktop.ui.payment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import java.math.BigDecimal

/**
 * Чем платят за чек — одним видом или несколькими сразу.
 *
 * Покупатель приносит часть суммы картой, часть наличными: это обычный
 * расчёт в магазине, а не редкий случай. Протокол принимает список оплат,
 * и разбиение живёт здесь — одинаково для продажи и для возврата.
 *
 * Последняя оплата всегда берёт остаток и вводу не поддаётся. Так суммы
 * складываются в итог по построению: кассиру нечего сводить в уме, а узлу
 * не приходит чек, в котором заплачено не столько, сколько пробито.
 */
class PaymentSplit(first: String = CASH_PAYMENT) {

    private val lines = mutableStateListOf(PaymentLine(first))

    /** Оплаты чека сверху вниз; последняя — остаток. */
    val entries: List<PaymentLine> get() = lines

    /** Оплат больше одной: суммы вводятся, а не подразумеваются. */
    val mixed: Boolean get() = lines.size > 1

    /** Виды оплаты, выбранные сейчас. */
    val types: List<String> get() = lines.map { it.type }

    /** Добавляет оплату указанного вида остатком чека. */
    fun add(type: String) {
        if (lines.none { it.type == type }) lines.add(PaymentLine(type))
    }

    /** Убирает оплату; последнюю оставшуюся убрать нельзя. */
    fun remove(line: PaymentLine) {
        if (lines.size > 1) lines.remove(line)
    }

    /** Меняет вид оплаты в строке, если такой ещё не выбран в другой. */
    fun retype(line: PaymentLine, type: String) {
        if (lines.none { it !== line && it.type == type }) line.type = type
    }

    /**
     * Сумма оплаты в строке.
     *
     * Единственная оплата — весь итог: спрашивать сумму, когда она и так
     * известна, значит требовать ввод ради ввода. Последняя из нескольких
     * забирает остаток.
     */
    fun sumOf(line: PaymentLine, total: BigDecimal): BigDecimal =
        if (line === lines.last()) total - assigned() else line.value ?: BigDecimal.ZERO

    /** Оплаты для узла. */
    fun toPayments(total: BigDecimal): List<ReceiptPayment> =
        lines.map { ReceiptPayment(it.type, sumOf(it, total)) }

    /** Наличная часть чека: от неё считается сдача. */
    fun cashSum(total: BigDecimal): BigDecimal =
        lines.filter { it.type == CASH_PAYMENT }.sumOf { sumOf(it, total) }

    /** Платят ли наличными хотя бы частью. */
    val hasCash: Boolean get() = lines.any { it.type == CASH_PAYMENT }

    /** Что уже расписано по видам, кроме остатка. */
    fun assigned(): BigDecimal =
        lines.dropLast(1).fold(BigDecimal.ZERO) { sum, line -> sum + (line.value ?: BigDecimal.ZERO) }

    /** Почему такое разбиение принять нельзя, или `null`. */
    fun issue(total: BigDecimal): SplitIssue? = when {
        !mixed -> null
        lines.dropLast(1).any { it.value == null || it.value!!.signum() <= 0 } -> SplitIssue.Empty
        assigned() >= total -> SplitIssue.Excess
        else -> null
    }

    /** Возвращает разбиение к одной оплате: следующий чек начинается с чистого. */
    fun reset() {
        val kept = lines.first().type
        lines.clear()
        lines.add(PaymentLine(kept))
    }
}

/**
 * Одна оплата: чем и сколько.
 *
 * Сумма хранится текстом, как её набрал кассир: разобранное число теряет
 * незаконченный ввод, и поле дёргалось бы под руками.
 */
class PaymentLine(type: String) {
    var type: String by mutableStateOf(type)
    var amount: String by mutableStateOf("")

    /** Введённая сумма или `null`, если поле пустое либо не число. */
    val value: BigDecimal? get() = amount.trim().replace(',', '.').toBigDecimalOrNull()
}

/** Чем разбиение оплаты не годится. */
enum class SplitIssue { Empty, Excess }

/** Вид оплаты по умолчанию: наличные встречаются чаще прочих. */
const val CASH_PAYMENT: String = "CASH"
