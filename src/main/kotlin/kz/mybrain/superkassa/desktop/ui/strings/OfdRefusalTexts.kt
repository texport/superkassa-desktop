package kz.mybrain.superkassa.desktop.ui.strings

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
)

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

/** Слова об отказе по его коду; `null` — такого кода приложение не знает. */
fun ofdRefusalWords(code: Int?, language: Language): String? {
    val known = OfdResult.entries.firstOrNull { it.code == code } ?: return null
    return known.words(ofdRefusalTexts(language))
}

fun ofdRefusalTexts(language: Language): OfdRefusalTexts = when (language) {
    Language.Kk -> ofdRefusalKk
    Language.Ru -> ofdRefusalRu
    Language.En -> ofdRefusalEn
}

private val ofdRefusalRu = OfdRefusalTexts(
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

private val ofdRefusalKk = OfdRefusalTexts(
    unknownId = "БФД бұл кассаны білмейді",
    invalidToken = "Касса токені жарамсыз: кабинеттен жаңасын алыңыз",
    protocolError = "БФД касса сұранысын талдамады",
    unknownCommand = "БФД мұндай пәрменді білмейді",
    unsupportedCommand = "БФД мұндай пәрменді қолдамайды",
    invalidConfiguration = "БФД касса баптауларын дұрыс емес деп санайды",
    invalidRequestNumber = "БФД күтпеген нөмірмен сұраныс алды",
    invalidRetry = "БФД сұраныс қайталауын қабылдамады",
    openShiftTimeout = "Ауысым тәуліктен ұзақ ашық: оны жабыңыз",
    incorrectData = "БФД чек деректерін қабылдамады",
    notEnoughCash = "БФД деректері бойынша ақша жәшігінде қолма-қол ақша жеткіліксіз",
    blocked = "БФД кассаны бұғаттады",
    sameTaxpayer = "Сатып алушы мен сатушы — бір тұлға",
    deregistered = "Касса есептен шығарылды",
    disconnected = "Касса БФД-дан ажыратылған",
    serviceUnavailable = "БФД уақытша қолжетімсіз",
    unknownError = "БФД себебін айтпай бас тартты"
)

private val ofdRefusalEn = OfdRefusalTexts(
    unknownId = "The BFD does not know this register",
    invalidToken = "The register token is invalid: issue a new one in the cabinet",
    protocolError = "The BFD could not parse the register request",
    unknownCommand = "The BFD does not know this command",
    unsupportedCommand = "The BFD does not support this command",
    invalidConfiguration = "The BFD considers the register settings invalid",
    invalidRequestNumber = "The BFD received a request with an unexpected number",
    invalidRetry = "The BFD rejected the request retry",
    openShiftTimeout = "The shift has been open for over a day: close it",
    incorrectData = "The BFD rejected the receipt data",
    notEnoughCash = "By the BFD records the cash drawer has not enough cash",
    blocked = "The BFD has blocked the register",
    sameTaxpayer = "The customer and the taxpayer are the same person",
    deregistered = "The register is deregistered",
    disconnected = "The register is disconnected from the BFD",
    serviceUnavailable = "The BFD is temporarily unavailable",
    unknownError = "The BFD refused without an explanation"
)
