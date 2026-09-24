package kz.mybrain.superkassa.strings.impl.debug

import kz.mybrain.superkassa.strings.api.debug.DebugTexts

/** Надписи [DebugTexts] по-английски. */
internal val debugTextsEn = DebugTexts(
    title = "Application log",
    debugMode = "Debug mode",
    debugModeHint = "While it is on, the log window stays open next to the main one: " +
        "it shows what the till and the cabinet do and what they answer",
    level = "Log level",
    levelDebug = "Debug",
    levelInfo = "Normal",
    levelWarning = "Warnings",
    levelFailure = "Failures",
    search = "Search in lines",
    clear = "Clear",
    save = "Save to file",
    lines = "Lines",
    empty = "The log is empty",
    emptyHint = "Every till action and every call to the cabinet, every error and every warning " +
        "will show up here",
    file = "Log file",
    secretsHint = "The cashier PIN, the till token, the signing password, the signature and " +
        "buyer data never reach the log, at any level",
    sourceCabinet = "Cabinet",
    sourceMachine = "Till",
    sourceSignature = "Signature",
    sourceApp = "Application"
)
