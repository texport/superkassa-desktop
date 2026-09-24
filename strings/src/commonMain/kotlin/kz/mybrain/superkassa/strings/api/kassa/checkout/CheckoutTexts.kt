package kz.mybrain.superkassa.strings.api.kassa.checkout

/**
 * Надписи расчёта: блок оплаты, итог пробитого чека и подтверждение возврата.
 *
 * @property toPay подпись главного числа блока оплаты.
 * @property issued заголовок итога пробитого чека.
 * @property issuedSum подпись суммы пробитого чека.
 * @property change подпись сдачи покупателю.
 * @property showReceipt открыть пробитый чек на экране.
 * @property printReceipt распечатать пробитый чек.
 * @property nextReceipt убрать итог и начать следующий чек.
 * @property confirmGive вопрос перед возвратом продажи; `%s` — сумма.
 * @property confirmTake вопрос перед возвратом покупки; `%s` — сумма.
 * @property confirmBasis последствие возврата; первое `%s` — номер
 *   чека-основания, второе — его итог.
 * @property confirmCancel не возвращать.
 * @property refundDone итог возврата; первое `%s` — вид возврата, второе — сумма.
 * @property refundPart подсказка под полем суммы возврата: коротко, в одну
 *   строку — поле стоит в узкой панели, и длинная подсказка уходила под сгиб.
 */
data class CheckoutTexts(
    val toPay: String,
    val issued: String,
    val issuedSum: String,
    val change: String,
    val showReceipt: String,
    val printReceipt: String,
    val nextReceipt: String,
    val confirmGive: String,
    val confirmTake: String,
    val confirmBasis: String,
    val confirmCancel: String,
    val refundDone: String,
    val refundPart: String
)
