package kz.mybrain.superkassa.presentation.strings.debug

import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.presentation.strings.common.Language

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

/** Надписи журнала на выбранном языке. */
fun debugTexts(language: Language): DebugTexts = when (language) {
    Language.Kk -> debugTextsKk
    Language.Ru -> debugTextsRu
    Language.En -> debugTextsEn
}

/** Как называется уровень записи на языке читающего. */
fun DebugTexts.name(level: LogLevel): String = when (level) {
    LogLevel.Debug -> levelDebug
    LogLevel.Info -> levelInfo
    LogLevel.Warning -> levelWarning
    LogLevel.Failure -> levelFailure
}

/** Как называется источник записи на языке читающего. */
fun DebugTexts.name(source: LogSource): String = when (source) {
    LogSource.Cabinet -> sourceCabinet
    LogSource.Machine -> sourceMachine
    LogSource.Signature -> sourceSignature
    LogSource.App -> sourceApp
}

private val debugTextsRu = DebugTexts(
    title = "Журнал приложения",
    debugMode = "Режим отладки",
    debugModeHint = "Пока он включён, рядом с главным окном открыто окно журнала: " +
        "видно, что касса и кабинет делают и что отвечают",
    level = "Уровень записи",
    levelDebug = "Отладочный",
    levelInfo = "Обычный",
    levelWarning = "Предупреждения",
    levelFailure = "Отказы",
    search = "Поиск по строке",
    clear = "Очистить",
    save = "Сохранить в файл",
    lines = "Строк",
    empty = "Журнал пуст",
    emptyHint = "Здесь появится каждое действие кассы и обращение к кабинету, " +
        "каждая ошибка и каждое предупреждение",
    file = "Файл журнала",
    secretsHint = "Пин кассира, токен кассы, пароль ЭЦП, подпись и данные покупателя " +
        "в журнал не попадают ни на каком уровне",
    sourceCabinet = "Кабинет",
    sourceMachine = "Касса",
    sourceSignature = "ЭЦП",
    sourceApp = "Приложение"
)

private val debugTextsKk = DebugTexts(
    title = "Бағдарлама журналы",
    debugMode = "Жөндеу режимі",
    debugModeHint = "Қосулы тұрғанда журнал терезесі басты терезенің қасында ашық болады: " +
        "касса мен кабинеттің не істейтіні және не жауап беретіні көрінеді",
    level = "Жазу деңгейі",
    levelDebug = "Жөндеу",
    levelInfo = "Кәдімгі",
    levelWarning = "Ескертулер",
    levelFailure = "Бас тартулар",
    search = "Жол бойынша іздеу",
    clear = "Тазалау",
    save = "Файлға сақтау",
    lines = "Жолдар",
    empty = "Журнал бос",
    emptyHint = "Мұнда кассаның әрбір әрекеті мен кабинетке жасалған әрбір сұрау, " +
        "әрбір қате мен ескерту көрінеді",
    file = "Журнал файлы",
    secretsHint = "Кассир піні, касса токені, ЭЦҚ құпиясөзі, қолтаңба және сатып алушының " +
        "деректері журналға ешбір деңгейде түспейді",
    sourceCabinet = "Кабинет",
    sourceMachine = "Касса",
    sourceSignature = "ЭЦҚ",
    sourceApp = "Бағдарлама"
)

private val debugTextsEn = DebugTexts(
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
