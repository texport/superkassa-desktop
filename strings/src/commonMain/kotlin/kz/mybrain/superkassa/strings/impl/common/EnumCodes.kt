package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.EnumTexts

/** Название вида оплаты по коду узла; `null` — код незнаком. */
internal fun paymentName(texts: EnumTexts, code: String): String? = when (code) {
    "CASH" -> texts.paymentCash
    "CARD" -> texts.paymentCard
    "ELECTRONIC" -> texts.paymentElectronic
    "MOBILE" -> texts.paymentMobile
    "CREDIT" -> texts.paymentCredit
    "TARE" -> texts.paymentTare
    else -> null
}

/** Название типа документа по коду журнала; `null` — код незнаком. */
internal fun documentName(texts: EnumTexts, code: String): String? = when (code) {
    "CHECK", "TICKET", "RECEIPT" -> texts.docCheck
    "SALE", "SELL" -> texts.docSale
    "RETURN", "SALE_RETURN", "SELL_RETURN" -> texts.docReturn
    "BUY" -> texts.docBuy
    "BUY_RETURN" -> texts.docBuyReturn
    "REPORT_X", "X_REPORT" -> texts.docReportX
    "SHIFT_OPEN" -> texts.docShiftOpen
    "SHIFT_CLOSE", "Z_REPORT", "REPORT_Z" -> texts.docShiftClose
    "CASH_IN" -> texts.docCashIn
    "CASH_OUT" -> texts.docCashOut
    else -> null
}
