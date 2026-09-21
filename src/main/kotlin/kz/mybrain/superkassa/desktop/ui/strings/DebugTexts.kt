package kz.mybrain.superkassa.desktop.ui.strings

import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource

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
    val sourceNode: String,
    val sourceCabinet: String,
    val sourceMachine: String,
    val sourceSignature: String,
    val sourceApp: String,

    /** Вывод самого узла, дочитанный из его файла. */
    val sourceNodeSelf: String
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
    LogSource.Node -> sourceNode
    LogSource.Cabinet -> sourceCabinet
    LogSource.Machine -> sourceMachine
    LogSource.Signature -> sourceSignature
    LogSource.App -> sourceApp
    LogSource.NodeSelf -> sourceNodeSelf
}

private val debugTextsRu = DebugTexts(
    title = "Журнал приложения",
    debugMode = "Режим отладки",
    debugModeHint = "Пока он включён, рядом с главным окном открыто окно журнала: " +
        "видно, что уходит к узлу и в кабинет и что оттуда приходит",
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
    emptyHint = "Здесь появится каждое обращение к узлу и к кабинету, " +
        "каждая ошибка и каждое предупреждение",
    file = "Файл журнала",
    secretsHint = "Пин кассира, токен кассы, пароль ЭЦП, подпись и данные покупателя " +
        "в журнал не попадают ни на каком уровне",
    sourceNode = "Узел",
    sourceCabinet = "Кабинет",
    sourceMachine = "Касса",
    sourceSignature = "ЭЦП",
    sourceApp = "Приложение",
    sourceNodeSelf = "Узел изнутри"
)

private val debugTextsKk = DebugTexts(
    title = "Бағдарлама журналы",
    debugMode = "Жөндеу режимі",
    debugModeHint = "Қосулы тұрғанда журнал терезесі басты терезенің қасында ашық болады: " +
        "түйінге және кабинетке не кететіні, олардан не келетіні көрінеді",
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
    emptyHint = "Мұнда түйінге және кабинетке жасалған әрбір сұрау, " +
        "әрбір қате мен ескерту көрінеді",
    file = "Журнал файлы",
    secretsHint = "Кассир піні, касса токені, ЭЦҚ құпиясөзі, қолтаңба және сатып алушының " +
        "деректері журналға ешбір деңгейде түспейді",
    sourceNode = "Түйін",
    sourceCabinet = "Кабинет",
    sourceMachine = "Касса",
    sourceSignature = "ЭЦҚ",
    sourceApp = "Бағдарлама",
    sourceNodeSelf = "Түйін ішінен"
)

private val debugTextsEn = DebugTexts(
    title = "Application log",
    debugMode = "Debug mode",
    debugModeHint = "While it is on, the log window stays open next to the main one: " +
        "it shows what goes to the node and to the cabinet and what comes back",
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
    emptyHint = "Every call to the node and to the cabinet, every error and every warning " +
        "will show up here",
    file = "Log file",
    secretsHint = "The cashier PIN, the till token, the signing password, the signature and " +
        "buyer data never reach the log, at any level",
    sourceNode = "Node",
    sourceCabinet = "Cabinet",
    sourceMachine = "Till",
    sourceSignature = "Signature",
    sourceApp = "Application",
    sourceNodeSelf = "Node internals"
)
