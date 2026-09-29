package kz.mybrain.superkassa.presentation.common.print

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse

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
object PrintFileName {

    /** Документ кассы: вид, номер смены и фискальный признак. */
    fun of(document: FiscalDocumentResponse): String =
        ofDocument(document.docType, document.shiftNo, document.fiscalSign, document.autonomousSign)

    /**
     * Документ журнала по его полям: вид, номер смены и фискальный признак,
     * а у автономного документа — его автономный.
     */
    fun ofDocument(docType: String?, shiftNo: Long?, fiscalSign: String?, autonomousSign: String?): String =
        buildString {
            append(kindOf(docType))
            shiftNo?.let { append("-shift-").append(it) }
            (fiscalSign?.takeIf { it.isNotBlank() } ?: autonomousSign?.takeIf { it.isNotBlank() })
                ?.let { append('-').append(it) }
        }

    /** Z-отчёт закрытой смены: у него есть только номер смены. */
    fun zReport(shiftNo: Long?): String = of(SHIFT_CLOSE, number = null, shiftNo = shiftNo)

    /** Документ кабинета: вид и номер из строки журнала. */
    fun of(typeCode: String?, number: String?, shiftNo: Long?): String = buildString {
        append(kindOf(typeCode))
        shiftNo?.let { append("-shift-").append(it) }
        number?.takeIf { it.isNotBlank() && it.any(Char::isLetterOrDigit) }
            ?.let { append('-').append(it.filter(Char::isLetterOrDigit)) }
    }

    /**
     * Вид документа латиницей; незнакомый — словом «document».
     *
     * Видов по нескольку имён, потому что источников два: касса называет
     * их кодами протокола, кабинет — своими словами. Кабинетные
     * `PURCHASE`, `DEPOSIT` и `WITHDRAWAL` имя файла прежде не знало,
     * и покупка, внесение и изъятие сохранялись одинаковым «document».
     */
    private fun kindOf(docType: String?): String = when (docType?.uppercase()) {
        "SALE", "SELL", "TICKET" -> "receipt-sale"
        "SALE_RETURN", "SELL_RETURN", "RETURN" -> "receipt-sale-return"
        "BUY", "PURCHASE" -> "receipt-buy"
        "BUY_RETURN", "PURCHASE_RETURN" -> "receipt-buy-return"
        "CASH_IN", "DEPOSIT", "MONEY_PLACEMENT_DEPOSIT" -> "cash-in"
        "CASH_OUT", "WITHDRAWAL", "MONEY_PLACEMENT_WITHDRAWAL" -> "cash-out"
        "X_REPORT", "REPORT_X", "X" -> "x-report"
        "Z_REPORT", "REPORT_Z", "Z", "SHIFT_CLOSE" -> "z-report"
        "SHIFT_OPEN" -> "shift-open"
        else -> "document"
    }

    /** Вид документа закрытия смены, как его называет касса. */
    private const val SHIFT_CLOSE = "SHIFT_CLOSE"
}
