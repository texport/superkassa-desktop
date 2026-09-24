package kz.mybrain.superkassa.data.cabinet.documents

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.data.cabinet.toCabinet
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovement
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovementDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetPage
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceiptDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReport
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReportDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentPeriod
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentsOverview
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReceiptSearch
import kz.mybrain.superkassa.domain.cabinet.port.CabinetDocuments
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.DocumentsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage as BfdPage
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.DocumentPeriod as BfdPeriod
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.ReceiptSearch as BfdSearch

/** Документы касс — модулем кабинета от имени вошедшего. */
internal class RemoteDocuments(private val documents: DocumentsApi) : CabinetDocuments {
    override suspend fun overview(registerId: String): DocumentsOverview =
        cabinetCall { documents.overview(registerId) }.overview()

    override suspend fun receipts(registerId: String, search: ReceiptSearch): CabinetPage<CabinetReceipt> =
        cabinetCall { documents.receipts(registerId, search.sent()) }.page { it.receipt() }

    override suspend fun shifts(registerId: String, page: Int): CabinetPage<CabinetShift> =
        cabinetCall { documents.shifts(registerId, page) }.page { it.shift() }

    override suspend fun reports(registerId: String, page: Int, period: DocumentPeriod): CabinetPage<CabinetReport> =
        cabinetCall { documents.reports(registerId, page, period.sent()) }.page { it.report() }

    override suspend fun movements(
        registerId: String,
        page: Int,
        period: DocumentPeriod
    ): CabinetPage<CabinetCashMovement> =
        cabinetCall { documents.movements(registerId, page, period.sent()) }.page { it.movement() }

    override suspend fun receipt(registerId: String, transactionId: String): CabinetReceiptDetails =
        cabinetCall { documents.receipt(registerId, transactionId) }.details()

    override suspend fun report(registerId: String, transactionId: String): CabinetReportDetails =
        cabinetCall { documents.report(registerId, transactionId) }.details()

    override suspend fun movement(registerId: String, transactionId: String): CabinetCashMovementDetails =
        cabinetCall { documents.movement(registerId, transactionId) }.details()
}

private fun <T, R> BfdPage<T>.page(each: (T) -> R) = CabinetPage(page, size, totalElements, items.map(each))

private fun DocumentPeriod.sent() = BfdPeriod(from, to)

private fun ReceiptSearch.sent() = BfdSearch(
    page = page,
    size = size,
    receiptNumber = receiptNumber,
    shiftNumber = shiftNumber,
    sumFrom = sumFrom?.toCabinet(),
    sumTo = sumTo?.toCabinet(),
    operationTypes = operationTypes,
    dateFrom = dateFrom,
    dateTo = dateTo
)
