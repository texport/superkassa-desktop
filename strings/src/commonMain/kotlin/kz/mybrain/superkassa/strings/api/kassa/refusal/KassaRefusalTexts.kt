package kz.mybrain.superkassa.strings.api.kassa.refusal

import kz.mybrain.superkassa.strings.impl.kassa.refusal.kassaRefusalOwnWords

/**
 * Свои слова отказов кассы.
 *
 * Правило выбора между словами ядра и своими — у приложения: оно знает
 * отказ кассы целиком. Здесь только слова и перечень кодов, для которых
 * они свои.
 *
 * @property unknown отказ без годных слов; `%s` — код отказа.
 */
data class KassaRefusalTexts(
    val programming: String,
    val vatNotPayer: String,
    val vatUnknown: String,
    val paymentUnsupported: String,
    val unitUnknown: String,
    val outOfRange: String,
    val basisRequired: String,
    val unknown: String
) {
    /**
     * Свои слова для отказа с этим кодом; `null` — слова ядра годятся кассиру.
     *
     * Свои слова нужны там, где слова ядра не годятся кассиру: в них код
     * ставки, вида оплаты или единицы («VAT_12», «MOBILE», «999»), имя поля
     * запроса («parentTicket», «items[0].price») или нет того, что делать.
     */
    fun own(code: String): String? = kassaRefusalOwnWords(this, code)
}
