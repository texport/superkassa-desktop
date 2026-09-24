package kz.mybrain.superkassa.domain.cabinet.usecase.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetPage
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentPeriod
import kz.mybrain.superkassa.domain.cabinet.model.documents.ReceiptSearch
import kz.mybrain.superkassa.domain.cabinet.port.CabinetDocuments

/**
 * Страница документов выбранного вида.
 *
 * Виды кабинет отдаёт разными списками: чеки — отбором с границами срока
 * в теле, отчёты и движение денег — параметрами, смены — без срока вовсе.
 */
class ReadDocumentsPage(private val documents: CabinetDocuments) {
    suspend operator fun invoke(
        registerId: String,
        kind: DocumentKind,
        page: Int,
        period: DocumentPeriod
    ): CabinetPage<*> =
        when (kind) {
            DocumentKind.Receipts ->
                documents.receipts(registerId, ReceiptSearch(page = page, dateFrom = period.from, dateTo = period.to))
            DocumentKind.Shifts -> documents.shifts(registerId, page)
            DocumentKind.Reports -> documents.reports(registerId, page, period)
            DocumentKind.CashMovements -> documents.movements(registerId, page, period)
        }
}
