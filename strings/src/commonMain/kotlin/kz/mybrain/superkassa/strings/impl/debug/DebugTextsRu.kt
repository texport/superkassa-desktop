package kz.mybrain.superkassa.strings.impl.debug

import kz.mybrain.superkassa.strings.api.debug.DebugTexts

/** Надписи [DebugTexts] по-русски. */
internal val debugTextsRu = DebugTexts(
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
