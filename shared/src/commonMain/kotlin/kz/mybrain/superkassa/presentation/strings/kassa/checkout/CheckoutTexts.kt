package kz.mybrain.superkassa.presentation.strings.kassa.checkout

import kz.mybrain.superkassa.presentation.strings.common.Language

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

/** Надписи расчёта на выбранном языке. */
fun checkoutTexts(language: Language): CheckoutTexts = when (language) {
    Language.Kk -> kazakhCheckout
    Language.Ru -> russianCheckout
    Language.En -> englishCheckout
}

private val russianCheckout = CheckoutTexts(
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

private val kazakhCheckout = CheckoutTexts(
    toPay = "Төлеуге",
    issued = "Чек басылды",
    issuedSum = "Чек сомасы",
    change = "Қайтарым",
    showReceipt = "Чекті көрсету",
    printReceipt = "Басып шығару",
    nextReceipt = "Келесі чек",
    confirmGive = "Сатып алушыға %s қайтарылсын ба?",
    confirmTake = "Сатушыдан %s қабылдансын ба?",
    confirmBasis = "№ %s чек бойынша, сомасы %s. Қайтару БФД-ға жіберіледі және болдырылмайды.",
    confirmCancel = "Бас тарту",
    refundDone = "%s: %s",
    refundPart = "Чек сомасынан аз — бөлігі қайтады"
)

private val englishCheckout = CheckoutTexts(
    toPay = "To pay",
    issued = "Receipt issued",
    issuedSum = "Receipt total",
    change = "Change",
    showReceipt = "Show receipt",
    printReceipt = "Print",
    nextReceipt = "Next receipt",
    confirmGive = "Give %s back to the customer?",
    confirmTake = "Take %s back from the seller?",
    confirmBasis = "Against receipt No. %s for %s. The refund goes to the BFD and cannot be undone.",
    confirmCancel = "Cancel",
    refundDone = "%s for %s",
    refundPart = "Less than the receipt refunds a part"
)
