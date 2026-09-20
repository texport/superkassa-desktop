package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

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
fun documentTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "SALE" -> texts.operationSale
    "RETURN" -> texts.operationReturn
    "BUY", "PURCHASE" -> texts.operationPurchase
    "BUY_RETURN", "PURCHASE_RETURN" -> texts.operationPurchaseReturn
    "Z" -> texts.reportZ
    "X" -> texts.reportX
    "DEPOSIT" -> texts.deposit
    "WITHDRAWAL" -> texts.withdrawal
    null -> ""
    else -> code
}

/**
 * Вид оплаты словами.
 *
 * В карточке чека стоял код протокола: `CASH`, `TARE`. Владелец читает
 * чек глазами покупателя, и «Тарой» ему понятно, а `TARE` — нет.
 */
fun paymentTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "CASH" -> texts.paymentCash
    "CARD" -> texts.paymentCard
    "ELECTRONIC" -> texts.paymentElectronic
    "MOBILE" -> texts.paymentMobile
    "CREDIT" -> texts.paymentCredit
    "TARE" -> texts.paymentTare
    null -> ""
    else -> code
}

/** Налог словами: в чеке стоял код ставки, а не её название. */
fun taxTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "VAT", "NDS" -> texts.taxVat
    null -> ""
    else -> code
}
