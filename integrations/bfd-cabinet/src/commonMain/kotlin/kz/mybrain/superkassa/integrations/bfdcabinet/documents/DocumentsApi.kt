package kz.mybrain.superkassa.integrations.bfdcabinet.documents

import io.ktor.http.HttpMethod
import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.query

/**
 * Фискальные документы кассы глазами ОФД: чеки, смены, отчёты и движение
 * денег — то, что принял сервер приёма, а не то, что лежит в кассе.
 *
 * Документы читаются страницами: чеков у кассы десятки тысяч, и все сразу
 * не нужны никому.
 */
class DocumentsApi internal constructor(private val link: CabinetLink) {

    /** Сколько чего накопила касса. */
    suspend fun overview(registerId: String): DocumentsOverview = link.get(base(registerId) + "/documents")

    /** Страница чеков по отбору; период уходит телом отбора. */
    suspend fun receipts(registerId: String, search: ReceiptSearch): CabinetPage<CabinetReceipt> =
        link.send(HttpMethod.Post, base(registerId) + "/receipts/search", search)

    /**
     * Страница смен. Периода у смен нет: кабинет по дате их не отбирает,
     * а отбор прочитанной страницы на месте показывал бы «за сегодня»
     * только то, что попало в первые пятьдесят строк.
     */
    suspend fun shifts(registerId: String, page: Int = 0): CabinetPage<CabinetShift> =
        link.get(base(registerId) + "/shifts" + query("page" to page, "size" to CABINET_PAGE_SIZE))

    /** Страница X- и Z-отчётов за период. */
    suspend fun reports(
        registerId: String,
        page: Int = 0,
        period: DocumentPeriod = DocumentPeriod()
    ): CabinetPage<CabinetReport> = link.get(base(registerId) + "/reports" + paged(page, period))

    /** Страница внесений и изъятий за период. */
    suspend fun movements(
        registerId: String,
        page: Int = 0,
        period: DocumentPeriod = DocumentPeriod()
    ): CabinetPage<CabinetCashMovement> = link.get(base(registerId) + "/cash-movements" + paged(page, period))

    /** Чек целиком: состав, оплаты, налоги, отметка КГД и пакет протокола. */
    suspend fun receipt(registerId: String, transactionId: String): CabinetReceiptDetails =
        link.get(base(registerId) + "/receipts/${transactionId.inPath()}")

    /** Отчёт целиком. */
    suspend fun report(registerId: String, transactionId: String): CabinetReportDetails =
        link.get(base(registerId) + "/reports/${transactionId.inPath()}")

    /** Внесение или изъятие целиком. */
    suspend fun movement(registerId: String, transactionId: String): CabinetCashMovementDetails =
        link.get(base(registerId) + "/cash-movements/${transactionId.inPath()}")

    /** Страница и период; пустой период не добавляет ни одного параметра. */
    private fun paged(page: Int, period: DocumentPeriod) =
        query("page" to page, "size" to CABINET_PAGE_SIZE, "dateFrom" to period.from, "dateTo" to period.to)

    private fun base(registerId: String) = "/api/cash-registers/${registerId.inPath()}"
}
