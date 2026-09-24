package kz.mybrain.superkassa.domain.cabinet.usecase.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.OpenedDocument
import kz.mybrain.superkassa.domain.cabinet.model.documents.RowTarget
import kz.mybrain.superkassa.domain.cabinet.port.CabinetDocuments

/**
 * Документ строки. Смена уже прочитана вместе со списком, остальное
 * кабинет отдаёт по идентификатору операции.
 */
class OpenDocument(private val documents: CabinetDocuments) {
    suspend operator fun invoke(registerId: String, kind: DocumentKind, target: RowTarget): OpenedDocument? =
        when (target) {
            is RowTarget.Local -> OpenedDocument.Shift(target.shift)
            is RowTarget.Remote -> when (kind) {
                DocumentKind.Receipts -> OpenedDocument.Receipt(documents.receipt(registerId, target.transactionId))
                DocumentKind.Reports -> OpenedDocument.Report(documents.report(registerId, target.transactionId))
                DocumentKind.CashMovements ->
                    OpenedDocument.Movement(documents.movement(registerId, target.transactionId))
                DocumentKind.Shifts -> null
            }
        }
}
