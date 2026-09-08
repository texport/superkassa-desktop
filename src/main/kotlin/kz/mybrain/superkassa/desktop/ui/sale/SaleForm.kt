package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.ui.payment.PaymentSplit
import java.math.BigDecimal
import java.util.UUID

/**
 * Введённое кассиром поверх корзины.
 *
 * Держится одним состоянием, чтобы части экрана не передавали друг другу
 * по восемь переменных, а ключ повтора жил ровно до принятого чека.
 */
class SaleForm {
    var operation: SaleOperation by mutableStateOf(SaleOperation.Sell)
    val split: PaymentSplit = PaymentSplit()
    var taken: String by mutableStateOf("")
    var discount: String by mutableStateOf("")
    var markup: String by mutableStateOf("")
    var customerBin: String by mutableStateOf("")
    var domain: DomainInput by mutableStateOf(DomainInput())
    var issuing: Boolean by mutableStateOf(false)
    var attemptKey: String by mutableStateOf(newAttemptKey())
        private set

    /**
     * Скидка и наценка на весь чек — взаимоисключающие: ОФД такой чек
     * отвергает, и кодек не даёт его даже собрать.
     */
    fun enterDiscount(text: String) {
        discount = text
        if (text.isNotEmpty()) markup = ""
    }

    fun enterMarkup(text: String) {
        markup = text
        if (text.isNotEmpty()) discount = ""
    }

    /** Что уходит в узел вместе с корзиной. */
    fun input(total: BigDecimal): ReceiptInput = ReceiptInput(
        operation = operation,
        payments = split.toPayments(total),
        cashSum = split.cashSum(total),
        taken = amount(taken).value,
        discount = amount(discount).value,
        markup = amount(markup).value,
        customerBin = customerBin,
        domain = domain,
        idempotencyKey = attemptKey
    )

    /**
     * После принятого чека всё введённое поверх корзины забывается, и новый
     * чек получает свой ключ повтора: иначе второй чек подряд узел счёл бы
     * повтором первого.
     */
    fun startNextReceipt() {
        taken = ""
        discount = ""
        markup = ""
        customerBin = ""
        // Вид отрасли на рабочем месте не меняется, а счёт, машина и карта
        // принадлежат покупателю: перенести их в следующий чек значило бы
        // выписать его на чужие реквизиты.
        domain = DomainInput(kind = domain.kind)
        split.reset()
        attemptKey = newAttemptKey()
    }
}

/**
 * Ключ повтора одной попытки.
 *
 * Не меняется, пока чек не принят: если ответ узла потерялся в дороге,
 * повтор с тем же ключом не применит фискальный эффект дважды.
 */
private fun newAttemptKey(): String = "desktop-" + UUID.randomUUID()
