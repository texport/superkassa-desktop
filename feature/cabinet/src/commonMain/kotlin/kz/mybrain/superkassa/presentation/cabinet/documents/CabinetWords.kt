package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Слова документов кабинета: что это, дошло ли и чем расплатились.
 *
 * Отделено от состояний кассы: там речь о её месте в реестре КГД, здесь —
 * о судьбе отдельного документа. В одном файле обе темы перестали
 * помещаться, и границы между ними не стало видно.
 */
/**
 * Вид операции, отчёта и движения денег — словами.
 *
 * В списках документов стояли коды протокола: `OPERATION_SELL`, `Z`,
 * `DEPOSIT`. Владельцу они не говорят ничего, а половину из них
 * и разработчик читает по справочнику.
 *
 * Покупка у населения — не продажа: касса не получает деньги, а выдаёт
 * их. Кабинет называет её `BUY`, как и касса в своём журнале; прежнее
 * `PURCHASE` оставлено рядом, потому что сторону кабинета правят прямо
 * сейчас и оба написания какое-то время будут встречаться. Название
 * у них одно на оба: второй перевод разошёлся бы с первым.
 */
internal fun documentTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "SALE" -> texts.documents.operationSale
    "RETURN" -> texts.documents.operationReturn
    "BUY", "PURCHASE" -> texts.documents.operationPurchase
    "BUY_RETURN", "PURCHASE_RETURN" -> texts.documents.operationPurchaseReturn
    "Z" -> texts.documents.reportZ
    "X" -> texts.documents.reportX
    "DEPOSIT" -> texts.documents.deposit
    "WITHDRAWAL" -> texts.documents.withdrawal
    null -> ""
    else -> code
}

/**
 * Вид оплаты словами.
 *
 * В карточке чека стоял код протокола: `CASH`, `TARE`. Владелец читает
 * чек глазами покупателя, и «Тарой» ему понятно, а `TARE` — нет.
 */
internal fun paymentTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "CASH" -> texts.documents.paymentCash
    "CARD" -> texts.documents.paymentCard
    "ELECTRONIC" -> texts.documents.paymentElectronic
    "MOBILE" -> texts.documents.paymentMobile
    "CREDIT" -> texts.documents.paymentCredit
    "TARE" -> texts.documents.paymentTare
    null -> ""
    else -> code
}

/** Налог словами: в чеке стоял код ставки, а не её название. */
internal fun taxTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "VAT", "NDS" -> texts.documents.taxVat
    null -> ""
    else -> code
}
