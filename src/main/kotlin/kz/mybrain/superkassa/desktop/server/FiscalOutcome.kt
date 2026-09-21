package kz.mybrain.superkassa.desktop.server

import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource

/**
 * Исход фискальной операции в журнале.
 *
 * Обычный уровень журнала пишет обращение: метод, путь, код и время.
 * Для фискальной операции этого мало: `POST …/shift/close -> 200 за 155 мс`
 * не говорит, ушёл ли Z-отчёт в ОФД или лёг в очередь. Разбирая случай
 * «смена закрылась, а в ОФД ничего не пришло», по такой строке сказать
 * нечего — а тела запросов пишутся только на отладочном уровне, и включать
 * его задним числом уже поздно.
 *
 * Поэтому исход записывается отдельной строкой: номер документа
 * и состояние доставки, как их вернул узел. Ни сумм, ни состава чека
 * здесь нет — они в самом документе, а журнал читает обслуживание.
 */
internal fun FiscalResult.logged(what: String): FiscalResult {
    AppLog.record(
        source = LogSource.Node,
        level = if (isDelivered) LogLevel.Info else LogLevel.Warning,
        text = listOfNotNull(
            "$what: документ ${documentId ?: Glyph.NONE}",
            "доставка ${deliveryStatus ?: Glyph.NONE}",
            deliveryError?.takeIf { it.isNotBlank() }?.let { "отказ доставки: $it" },
            code?.takeIf { it.isNotBlank() }?.let { "отказ: $it" }
        ).joinToString(", ")
    )
    return this
}

/** Чем обозначается «узел этого не вернул». */
private object Glyph {
    const val NONE = "—"
}
