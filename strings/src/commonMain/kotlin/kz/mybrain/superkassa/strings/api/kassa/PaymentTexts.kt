package kz.mybrain.superkassa.strings.api.kassa

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
    /**
     * Под суммой, которую касса считает сама: остаток итога после прочих
     * оплат. Прежде это было подписью поля — «Остаток», — и кассир не
     * понимал, что его набирать не нужно и откуда оно берётся.
     */
    val rest: String,
    val splitEmpty: String,
    val splitExcess: String,
    /** После отказа БФД: набранное не пропало, его исправляют и проводят снова. */
    val rejectedKept: String
)
