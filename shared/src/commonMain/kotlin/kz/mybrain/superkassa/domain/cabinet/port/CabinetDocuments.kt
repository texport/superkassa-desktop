package kz.mybrain.superkassa.domain.cabinet.port

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

/**
 * Документы кассы так, как их принял сервер приёма данных.
 *
 * Виды кабинет отдаёт разными списками; смен по сроку он не отбирает.
 */
interface CabinetDocuments {
    suspend fun overview(registerId: String): DocumentsOverview

    suspend fun receipts(registerId: String, search: ReceiptSearch): CabinetPage<CabinetReceipt>

    suspend fun shifts(registerId: String, page: Int): CabinetPage<CabinetShift>

    suspend fun reports(registerId: String, page: Int, period: DocumentPeriod): CabinetPage<CabinetReport>

    suspend fun movements(registerId: String, page: Int, period: DocumentPeriod): CabinetPage<CabinetCashMovement>

    suspend fun receipt(registerId: String, transactionId: String): CabinetReceiptDetails

    suspend fun report(registerId: String, transactionId: String): CabinetReportDetails

    suspend fun movement(registerId: String, transactionId: String): CabinetCashMovementDetails
}
