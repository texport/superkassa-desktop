package kz.mybrain.superkassa.strings.api.kassa

import kz.mybrain.superkassa.strings.api.kassa.checkout.CheckoutTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.BuyerContactTexts
import kz.mybrain.superkassa.strings.api.kassa.refusal.KassaRefusalTexts
import kz.mybrain.superkassa.strings.api.kassa.scan.CameraScanTexts

/**
 * Надписи области «Касса»: продажа, оплата, возврат и деньги.
 *
 * Область собирает формы своих экранов в одно место: кассир весь день
 * работает в ней, и каждая форма берётся отсюда по смыслу.
 */
data class KassaTexts(
    /** Деньги в ящике, кассиры и настройка кассы. */
    val money: MoneyTexts,
    /** Экран продажи. */
    val sale: SaleTexts,
    /** Оплата чека — одними словами при продаже и возврате. */
    val payment: PaymentTexts,
    /** Почему касса заблокирована. */
    val blockReason: BlockReasonTexts,
    /** Единицы измерения. */
    val units: UnitTexts,
    /** Контакт покупателя для доставки чека. */
    val contact: BuyerContactTexts,
    /** Итог и оплата чека. */
    val checkout: CheckoutTexts,
    /** Свои слова отказов кассы. */
    val refusal: KassaRefusalTexts,
    /** Сканер штрихкода камерой устройства. */
    val scan: CameraScanTexts
)
