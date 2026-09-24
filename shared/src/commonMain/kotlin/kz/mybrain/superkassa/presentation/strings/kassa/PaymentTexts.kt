package kz.mybrain.superkassa.presentation.strings.kassa

import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи области «Оплата чека».
 *
 * Своя область, а не часть продажи: теми же словами оплата набирается
 * при возврате, и разводить два перевода одной фразы значит однажды
 * поправить только один из них.
 */
data class PaymentTexts(
    val addPayment: String,
    val removePayment: String,
    val amount: String,
    val rest: String,
    val splitEmpty: String,
    val splitExcess: String,
    /** После отказа БФД: набранное не пропало, его исправляют и проводят снова. */
    val rejectedKept: String
)

val paymentTextsRu = PaymentTexts(
    addPayment = "Добавить оплату",
    removePayment = "Убрать оплату",
    amount = "Сумма",
    rest = "Остаток",
    splitEmpty = "Укажите сумму каждой оплаты, кроме последней",
    splitExcess = "Суммы оплат больше итога чека",
    rejectedKept = "Набранное осталось на экране: исправьте и проведите снова"
)

val paymentTextsKk = PaymentTexts(
    addPayment = "Төлем қосу",
    removePayment = "Төлемді алып тастау",
    amount = "Сома",
    rest = "Қалдық",
    splitEmpty = "Соңғысынан басқа әр төлемнің сомасын көрсетіңіз",
    splitExcess = "Төлемдер сомасы чек қорытындысынан асып тұр",
    rejectedKept = "Терілгені экранда қалды: түзетіп, қайта өткізіңіз"
)

val paymentTextsEn = PaymentTexts(
    addPayment = "Add payment",
    removePayment = "Remove payment",
    amount = "Amount",
    rest = "Remainder",
    splitEmpty = "Enter the amount of every payment except the last",
    splitExcess = "Payments add up to more than the receipt total",
    rejectedKept = "What you entered is still on screen: correct it and submit again"
)

/** Надписи области на выбранном языке. */
fun paymentTexts(language: Language): PaymentTexts = when (language) {
    Language.Kk -> paymentTextsKk
    Language.Ru -> paymentTextsRu
    Language.En -> paymentTextsEn
}
