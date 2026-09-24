package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.OfdRefusalTexts

/** Надписи [OfdRefusalTexts] по-русски. */
internal val ofdRefusalTextsRu = OfdRefusalTexts(
    unknownId = "БФД не знает эту кассу",
    invalidToken = "Токен кассы недействителен: получите новый в кабинете",
    protocolError = "БФД не разобрал запрос кассы",
    unknownCommand = "БФД не знает такую команду",
    unsupportedCommand = "БФД не поддерживает такую команду",
    invalidConfiguration = "БФД считает настройки кассы неверными",
    invalidRequestNumber = "БФД получил запрос с неожиданным номером",
    invalidRetry = "БФД не принял повтор запроса",
    openShiftTimeout = "Смена открыта дольше суток: закройте её",
    incorrectData = "БФД отверг данные чека",
    notEnoughCash = "В денежном ящике по данным БФД недостаточно наличных",
    blocked = "БФД заблокировал кассу",
    sameTaxpayer = "Покупатель и продавец — одно лицо",
    deregistered = "Касса снята с учёта",
    disconnected = "Касса отключена от БФД",
    serviceUnavailable = "БФД временно недоступен",
    unknownError = "БФД отказал без объяснения"
)
