package kz.mybrain.superkassa.strings.api.debug

/**
 * Надписи журнала приложения и режима отладки.
 *
 * Журнал читает не кассир, а владелец и поддержка — но читают они его
 * на своём языке, как и остальные экраны. Заведено своим файлом: строк
 * полтора десятка, и живут они вместе с окном журнала.
 */
data class DebugTexts(
    val title: String,
    val debugMode: String,
    val debugModeHint: String,
    val level: String,
    val levelDebug: String,
    val levelInfo: String,
    val levelWarning: String,
    val levelFailure: String,
    val search: String,
    val clear: String,
    val save: String,
    val lines: String,
    val empty: String,
    val emptyHint: String,
    val file: String,
    val secretsHint: String,
    val sourceCabinet: String,
    val sourceMachine: String,
    val sourceSignature: String,
    val sourceApp: String
)
