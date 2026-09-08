package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

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
 */
fun documentTitle(code: String?, texts: CabinetTexts): String = when (code) {
    "SALE" -> texts.operationSale
    "RETURN" -> texts.operationReturn
    "PURCHASE" -> texts.operationPurchase
    "PURCHASE_RETURN" -> texts.operationPurchaseReturn
    "Z" -> texts.reportZ
    "X" -> texts.reportX
    "DEPOSIT" -> texts.deposit
    "WITHDRAWAL" -> texts.withdrawal
    null -> ""
    else -> code
}

/**
 * Состояние доставки документа в ОФД словами.
 *
 * Отказ доставки назывался «в очереди»: он не подходил под признак
 * доставленного и падал в тот же остаток, что и ждущее отправки. Для
 * владельца это разные вещи — ждущее уйдёт само, отказанное не уйдёт.
 */
fun deliveryTitle(code: String?, texts: CabinetTexts): String = when {
    code.isNullOrBlank() -> ""
    deliveryRefused(code) -> texts.deliveryRefused
    deliveryDone(code) -> texts.delivered
    else -> texts.pending
}

/** Плашка доставки: те же три цвета, что и у состояний кабинета. */
@Composable
fun DeliveryChip(code: String?, texts: CabinetTexts) {
    val title = deliveryTitle(code, texts).takeIf { it.isNotBlank() } ?: return
    Chip(text = title, color = deliveryColor(code))
}

/** Цвет доставки. */
@Composable
private fun deliveryColor(code: String?): Color = when {
    code.isNullOrBlank() -> StatusColors.pending
    deliveryRefused(code) -> StatusColors.refused
    deliveryDone(code) -> StatusColors.delivered
    else -> StatusColors.pending
}

private fun deliveryDone(code: String): Boolean =
    code.contains("OK", ignoreCase = true) || code.equals("DELIVERED", ignoreCase = true)

private fun deliveryRefused(code: String): Boolean =
    listOf("FAIL", "ERROR", "REJECT").any { code.contains(it, ignoreCase = true) }

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
