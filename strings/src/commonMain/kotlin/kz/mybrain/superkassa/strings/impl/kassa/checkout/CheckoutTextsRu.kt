package kz.mybrain.superkassa.strings.impl.kassa.checkout

import kz.mybrain.superkassa.strings.api.kassa.checkout.CheckoutTexts

/** Надписи [CheckoutTexts] по-русски. */
internal val checkoutTextsRu = CheckoutTexts(
    toPay = "К оплате",
    issued = "Чек пробит",
    issuedSum = "Сумма чека",
    change = "Сдача",
    showReceipt = "Показать чек",
    printReceipt = "Печать",
    nextReceipt = "Следующий чек",
    confirmGive = "Вернуть покупателю %s?",
    confirmTake = "Принять от продавца %s?",
    confirmBasis = "По чеку № %s на %s. Возврат уходит в БФД и не отменяется.",
    confirmCancel = "Отмена",
    refundDone = "%s на %s",
    refundPart = "Меньше суммы чека — вернётся часть"
)
