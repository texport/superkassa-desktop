package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.OfdRefusalTexts

/** Надписи [OfdRefusalTexts] по-английски. */
internal val ofdRefusalTextsEn = OfdRefusalTexts(
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
