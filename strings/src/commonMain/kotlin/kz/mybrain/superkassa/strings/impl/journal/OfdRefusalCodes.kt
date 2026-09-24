package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.OfdRefusalTexts

/** Слова об отказе БФД по его коду; `null` — такого кода приложение не знает. */
internal fun ofdRefusalWords(texts: OfdRefusalTexts, code: Int?): String? =
    OfdResult.entries.firstOrNull { it.code == code }?.words?.invoke(texts)

/**
 * Код отказа БФД — как он назван в `ResultTypeEnum` спецификации CPCR.
 *
 * Перечисление, а не числа по месту: код 17 в теле функции читается
 * загадкой, а `SameTaxpayerAndCustomer` — тем, что произошло.
 */
private enum class OfdResult(val code: Int, val words: (OfdRefusalTexts) -> String) {
    UnknownId(1, { it.unknownId }),
    InvalidToken(2, { it.invalidToken }),
    ProtocolError(3, { it.protocolError }),
    UnknownCommand(4, { it.unknownCommand }),
    UnsupportedCommand(5, { it.unsupportedCommand }),
    InvalidConfiguration(6, { it.invalidConfiguration }),
    InvalidRequestNumber(8, { it.invalidRequestNumber }),
    InvalidRetryRequest(9, { it.invalidRetry }),
    OpenShiftTimeoutExpired(11, { it.openShiftTimeout }),
    IncorrectRequestData(13, { it.incorrectData }),
    NotEnoughCash(14, { it.notEnoughCash }),
    Blocked(15, { it.blocked }),
    SameTaxpayerAndCustomer(17, { it.sameTaxpayer }),
    Deregistered(18, { it.deregistered }),
    Disconnected(19, { it.disconnected }),
    ServiceTemporarilyUnavailable(254, { it.serviceUnavailable }),
    UnknownError(255, { it.unknownError })
}
