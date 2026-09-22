package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.ui.payment.PaymentSplit
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
    var discount: Adjustment by mutableStateOf(Adjustment())
        private set
    var markup: Adjustment by mutableStateOf(Adjustment())
        private set
    var customerBin: String by mutableStateOf("")
    var issuing: Boolean by mutableStateOf(false)
    var attemptKey: String by mutableStateOf(newAttemptKey())
        private set

    /**
     * Скидка и наценка на весь чек — взаимоисключающие: БФД такой чек
     * отвергает, и кодек не даёт его даже собрать.
     */
    fun enterDiscount(text: String) {
        discount = discount.copy(text = text)
        if (text.isNotEmpty()) markup = markup.cleared()
    }

    fun enterMarkup(text: String) {
        markup = markup.copy(text = text)
        if (text.isNotEmpty()) discount = discount.cleared()
    }

    /**
     * Смена способа ввода не стирает набранное.
     *
     * Кассир, набравший «500» и переключившийся на проценты, видит рядом
     * с полем, во что обратилось его число, и правит его сам. Стереть
     * набранное за него значило бы заставить набирать заново того, кто
     * просто промахнулся по знаку.
     */
    fun switchDiscount(unit: AdjustmentUnit) {
        discount = discount.copy(unit = unit)
    }

    fun switchMarkup(unit: AdjustmentUnit) {
        markup = markup.copy(unit = unit)
    }

    /**
     * Что уходит в узел вместе с корзиной.
     *
     * Скидка и наценка уходят суммой в тенге, даже когда кассир набрал
     * их процентом: процент узел принимает и сам, но посчитал бы его
     * своим порядком округления, и «Итого» на экране разошлось бы
     * с фискальным чеком на тиын.
     */
    fun input(basket: Basket): ReceiptInput {
        val discountSum = discount.sumOf(basket.total)
        val markupSum = markup.sumOf(basket.total)
        val total = basket.totalWith(discountSum, markupSum)
        return ReceiptInput(
            operation = operation,
            payments = split.toPayments(total),
            cashSum = split.cashSum(total),
            taken = amount(taken).value,
            discount = discountSum,
            markup = markupSum,
            customerBin = customerBin,
            idempotencyKey = attemptKey
        )
    }

    /**
     * После принятого чека всё введённое поверх корзины забывается, и новый
     * чек получает свой ключ повтора: иначе второй чек подряд узел счёл бы
     * повтором первого.
     */
    fun startNextReceipt() {
        taken = ""
        discount = discount.cleared()
        markup = markup.cleared()
        customerBin = ""
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
