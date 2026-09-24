package kz.mybrain.superkassa.strings.api.journal

import kz.mybrain.superkassa.strings.impl.journal.ofdRefusalWords

/**
 * Отказы БФД словами кассира.
 *
 * БФД отвечает кодом из `ResultTypeEnum` и пояснением по-английски —
 * «Same customer and taxpayer IIN». Для обслуживания это язык протокола
 * и его текст остаётся в журнале, а кассиру нужно то же самое его
 * словами: он стоит перед покупателем и решает, что делать с чеком.
 *
 * Здесь только те коды, которые касса получает в ответ на свои команды.
 * Незнакомый код доходит пояснением БФД: молчать об отказе хуже, чем
 * сказать о нём чужими словами.
 */
data class OfdRefusalTexts(
    val unknownId: String,
    val invalidToken: String,
    val protocolError: String,
    val unknownCommand: String,
    val unsupportedCommand: String,
    val invalidConfiguration: String,
    val invalidRequestNumber: String,
    val invalidRetry: String,
    val openShiftTimeout: String,
    val incorrectData: String,
    val notEnoughCash: String,
    val blocked: String,
    val sameTaxpayer: String,
    val deregistered: String,
    val disconnected: String,
    val serviceUnavailable: String,
    val unknownError: String
) {
    /**
     * Слова об отказе по коду `ResultTypeEnum`; `null` — такого кода приложение
     * не знает, и кассиру идёт пояснение БФД как есть.
     */
    fun words(code: Int?): String? = ofdRefusalWords(this, code)
}
