package kz.mybrain.superkassa.presentation.words.kassa

import kz.mybrain.superkassa.domain.kassa.model.sale.DomainField
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.FieldKind
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleBlock
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleOperation
import kz.mybrain.superkassa.strings.api.common.EnumTexts
import kz.mybrain.superkassa.strings.api.common.ReceiptTexts
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.kassa.PaymentTexts
import kz.mybrain.superkassa.strings.api.kassa.SaleTexts

/*
 * Слова продажи для правил предметной области.
 *
 * Правило называет причину значением, а не фразой: оно проверяется без
 * экрана и без языка. Здесь каждое значение получает слова кассира на его
 * языке — одно место на экран, настройки и проверки.
 */

/** Направление чека в речи кассира, а не протокола. */
fun SaleOperation.title(texts: ReceiptTexts): String = when (this) {
    SaleOperation.Sell -> texts.sale
    SaleOperation.Buy -> texts.purchase
}

/** Надпись главной кнопки: «Пробить продажу». */
fun SaleOperation.action(texts: ReceiptTexts): String = when (this) {
    SaleOperation.Sell -> texts.issueSale
    SaleOperation.Buy -> texts.issuePurchase
}

/** Вид отрасли словами владельца: так он назван в настройках рабочего места. */
fun DomainKind.title(texts: EnumTexts): String = when (this) {
    DomainKind.Trading -> texts.domainTrading
    DomainKind.Services -> texts.domainServices
    DomainKind.Hotels -> texts.domainHotels
    DomainKind.GasOil -> texts.domainGasOil
    DomainKind.Taxi -> texts.domainTaxi
    DomainKind.Parking -> texts.domainParking
}

/** Подпись отраслевого поля — тем же словом, что на экране продажи. */
fun DomainField.label(texts: SaleTexts): String = when (this) {
    DomainField.AccountNumber -> texts.accountNumber
    DomainField.CardNumber -> texts.cardNumber
    DomainField.CarNumber -> texts.carNumber
    DomainField.Fee -> texts.fee
    DomainField.ParkingFrom -> texts.parkingFrom
    DomainField.ParkingTo -> texts.parkingTo
}

/**
 * Чего не хватает — словами кассира и с именем поля.
 *
 * «Заполните: Тариф» над заполненным полем со словом «Городской» —
 * это не причина, а загадка: такому полю сказано, что от него нужно
 * число, а полю времени — что нужны часы и минуты.
 */
fun DomainField.reason(texts: SaleTexts): String = when (typed) {
    FieldKind.Number -> texts.numberField.fill(label(texts))
    FieldKind.Time -> texts.timeField.fill(label(texts))
    FieldKind.Text -> "${texts.fillIn}: ${label(texts)}"
}

/**
 * Причина, по которой чек не пробить, словами кассира.
 *
 * Незаполненный отраслевой реквизит называется поимённо: «заполните
 * реквизиты» не говорит кассиру, какое поле пустует, а полей у такси
 * два.
 */
fun SaleBlock.reason(texts: SaleTexts, payment: PaymentTexts, field: DomainField? = null): String =
    if (this == SaleBlock.DomainFields && field != null) {
        field.reason(texts)
    } else {
        tillWords(texts) ?: moneyWords(texts, payment)
    }

/** Касса, смена и состав чека; `null` — причина о деньгах. */
private fun SaleBlock.tillWords(sale: SaleTexts): String? = when (this) {
    SaleBlock.NoKkm -> sale.blockNoKkm
    SaleBlock.NoPin -> sale.blockNoPin
    SaleBlock.KkmBlocked -> sale.blockKkmBlocked
    SaleBlock.ShiftClosed -> sale.blockShiftClosed
    SaleBlock.EmptyBasket -> sale.blockEmptyBasket
    SaleBlock.ZeroLine -> sale.blockZeroLine
    SaleBlock.DomainFields -> sale.fillIn
    SaleBlock.CustomerBin -> sale.blockBin
    SaleBlock.CustomerContact -> sale.blockContact
    else -> null
}

/** Оплата, скидки, итог и принятые деньги. */
private fun SaleBlock.moneyWords(sale: SaleTexts, payment: PaymentTexts): String = when (this) {
    SaleBlock.PaymentUnsupported -> sale.blockPaymentUnsupported
    SaleBlock.PaymentSplitEmpty -> payment.splitEmpty
    SaleBlock.PaymentSplitExcess -> payment.splitExcess
    SaleBlock.DiscountScopes -> sale.blockDiscountScopes
    SaleBlock.ChangeNotANumber -> sale.blockChangeNotANumber
    SaleBlock.DiscountNegative -> sale.blockDiscountNegative
    SaleBlock.PercentOverHundred -> sale.blockPercentRange
    SaleBlock.DiscountOverItems -> sale.blockDiscountOverItems
    SaleBlock.TotalNotPositive -> sale.blockTotalNotPositive
    SaleBlock.TakenTooSmall -> sale.blockTakenTooSmall
    else -> sale.fillIn
}
