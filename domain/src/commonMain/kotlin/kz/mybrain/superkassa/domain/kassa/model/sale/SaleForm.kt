package kz.mybrain.superkassa.domain.kassa.model.sale

import kz.mybrain.superkassa.domain.kassa.model.BuyerContact
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.model.ReceiptVat
import kz.mybrain.superkassa.domain.kassa.model.VatScope
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit

/**
 * Введённое кассиром поверх корзины.
 *
 * Неизменяемое и одно на чек: части экрана не передают друг другу
 * по восемь переменных, а модель продажи держит его вместе с корзиной
 * и ключом попытки — уход в другой раздел не стирает ничего.
 *
 * Здесь же стоит способ НДС чека — на весь чек одной ставкой или
 * по позициям: как скидка и наценка, это решение о чеке целиком,
 * а не о строке. Правило способов — в [ReceiptVat].
 *
 * @property contact контакт покупателя: по нему ему уходит чек.
 * @property domain отраслевые реквизиты этого чека. Вида отрасли здесь
 *   нет — он настройка кассы; здесь только набранное для этого чека.
 */
data class SaleForm(
    val operation: SaleOperation = SaleOperation.Sell,
    val split: PaymentSplit = PaymentSplit(),
    val taken: String = "",
    val discount: Adjustment = Adjustment(),
    val markup: Adjustment = Adjustment(),
    val customerBin: String = "",
    val contact: BuyerContact = BuyerContact(),
    val domain: DomainInput = DomainInput(),
    val vat: ReceiptVat = ReceiptVat()
) {
    /**
     * Скидка и наценка на весь чек — взаимоисключающие: БФД такой чек
     * отвергает, и касса не даёт его даже собрать.
     */
    fun enterDiscount(text: String): SaleForm =
        copy(discount = discount.copy(text = text), markup = if (text.isEmpty()) markup else markup.cleared())

    fun enterMarkup(text: String): SaleForm =
        copy(markup = markup.copy(text = text), discount = if (text.isEmpty()) discount else discount.cleared())

    /**
     * Смена способа ввода не стирает набранное: кассир видит рядом
     * с полем, во что обратилось его число, и правит его сам.
     */
    fun switchDiscount(unit: AdjustmentUnit): SaleForm = copy(discount = discount.copy(unit = unit))

    fun switchMarkup(unit: AdjustmentUnit): SaleForm = copy(markup = markup.copy(unit = unit))

    /** НДС на весь чек или по позициям: набранные у позиций ставки не стираются. */
    fun switchVat(scope: VatScope): SaleForm = copy(vat = vat.switchTo(scope))

    /** Ставка на весь чек. */
    fun chooseVat(rate: String): SaleForm = copy(vat = vat.choose(rate))

    /** ИИН/БИН покупателя: только цифры и не длиннее двенадцати. */
    fun enterBin(text: String): SaleForm = copy(customerBin = text.filter(Char::isDigit).take(BIN_LENGTH))

    /** Контакт покупателя, как его набрал кассир: разбирает его [BuyerContact]. */
    fun enterContact(text: String): SaleForm = copy(contact = contact.enter(text))

    fun chooseContactKind(kind: ContactKind): SaleForm = copy(contact = contact.switchTo(kind))

    /**
     * После принятого чека всё введённое поверх корзины забывается.
     *
     * Направление, способ ввода скидок и способ НДС остаются: следующий
     * чек обычно такой же. Отраслевые реквизиты и контакт принадлежат покупателю
     * и этой покупке — перенесённые, они выписали бы следующий чек на чужие
     * и отправили бы его чужому человеку. Вид контакта остаётся.
     */
    fun next(): SaleForm = SaleForm(
        operation = operation,
        split = split.reset(),
        discount = discount.cleared(),
        markup = markup.cleared(),
        contact = BuyerContact(contact.kind),
        vat = vat
    )
}
