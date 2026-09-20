package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.Document

/**
 * Как назвать файл печатной формы.
 *
 * Прежде в окно сохранения подставлялся идентификатор документа —
 * тридцать шесть знаков с дефисами. Такой файл владелец не найдёт
 * у себя через неделю и не отличит один чек от другого, а показать
 * его в КГД тем более нечем.
 *
 * Имя строится латиницей: файл уезжает на чужие машины и в почту,
 * где кириллица в имени доживает не всегда.
 */
internal object PrintFileName {

    /** Документ узла: вид, номер смены и фискальный признак. */
    fun of(document: Document): String = buildString {
        append(kindOf(document.docType))
        document.shiftNo?.let { append("-shift-").append(it) }
        signOf(document)?.let { append('-').append(it) }
    }

    /** Документ кабинета: вид и номер из строки журнала. */
    fun of(typeCode: String?, number: String?, shiftNo: Long?): String = buildString {
        append(kindOf(typeCode))
        shiftNo?.let { append("-shift-").append(it) }
        number?.takeIf { it.isNotBlank() && it.any(Char::isLetterOrDigit) }
            ?.let { append('-').append(it.filter(Char::isLetterOrDigit)) }
    }

    /** Вид документа латиницей; незнакомый — словом «document». */
    private fun kindOf(docType: String?): String = when (docType?.uppercase()) {
        "SALE", "SELL", "TICKET" -> "receipt-sale"
        "SALE_RETURN", "SELL_RETURN", "RETURN" -> "receipt-sale-return"
        "BUY" -> "receipt-buy"
        "BUY_RETURN" -> "receipt-buy-return"
        "CASH_IN", "MONEY_PLACEMENT_DEPOSIT" -> "cash-in"
        "CASH_OUT", "MONEY_PLACEMENT_WITHDRAWAL" -> "cash-out"
        "X_REPORT", "REPORT_X", "X" -> "x-report"
        "Z_REPORT", "REPORT_Z", "Z", "SHIFT_CLOSE" -> "z-report"
        "SHIFT_OPEN" -> "shift-open"
        else -> "document"
    }

    /** Фискальный признак, а у автономного документа — его автономный. */
    private fun signOf(document: Document): String? =
        document.fiscalSign?.takeIf { it.isNotBlank() }
            ?: document.autonomousSign?.takeIf { it.isNotBlank() }
}
