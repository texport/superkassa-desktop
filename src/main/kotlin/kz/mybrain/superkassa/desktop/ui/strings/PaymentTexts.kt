package kz.mybrain.superkassa.desktop.ui.strings

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
    val splitExcess: String
)

val paymentTextsRu = PaymentTexts(
    addPayment = "Добавить оплату",
    removePayment = "Убрать оплату",
    amount = "Сумма",
    rest = "Остаток",
    splitEmpty = "Укажите сумму каждой оплаты, кроме последней",
    splitExcess = "Суммы оплат больше итога чека"
)

val paymentTextsKk = PaymentTexts(
    addPayment = "Төлем қосу",
    removePayment = "Төлемді алып тастау",
    amount = "Сома",
    rest = "Қалдық",
    splitEmpty = "Соңғысынан басқа әр төлемнің сомасын көрсетіңіз",
    splitExcess = "Төлемдер сомасы чек қорытындысынан асып тұр"
)

val paymentTextsEn = PaymentTexts(
    addPayment = "Add payment",
    removePayment = "Remove payment",
    amount = "Amount",
    rest = "Remainder",
    splitEmpty = "Enter the amount of every payment except the last",
    splitExcess = "Payments add up to more than the receipt total"
)

/** Надписи области на выбранном языке. */
fun paymentTexts(language: Language): PaymentTexts = when (language) {
    Language.Kk -> paymentTextsKk
    Language.Ru -> paymentTextsRu
    Language.En -> paymentTextsEn
}
