package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.data.cabinet.CabinetCashMovementDetails
import kz.mybrain.superkassa.data.cabinet.CabinetReceiptDetails
import kz.mybrain.superkassa.data.cabinet.CabinetReportDetails
import kz.mybrain.superkassa.data.cabinet.CabinetShift
import kz.mybrain.superkassa.data.cabinet.cashMovement
import kz.mybrain.superkassa.data.cabinet.receipt
import kz.mybrain.superkassa.data.cabinet.report
import kz.mybrain.superkassa.presentation.session.CabinetSession

/**
 * Раскрытый документ кабинета.
 *
 * Раскрытым бывает любой вид, а не только чек: отчёт объясняет итог смены,
 * движение денег — кто и когда взял из ящика. Прежде нажатие на них
 * не делало ничего, и владелец решал, что список сломан.
 */
sealed interface OpenedDocument {
    data class Receipt(val details: CabinetReceiptDetails) : OpenedDocument
    data class Report(val details: CabinetReportDetails) : OpenedDocument
    data class Movement(val details: CabinetCashMovementDetails) : OpenedDocument
    data class Shift(val shift: CabinetShift) : OpenedDocument
}

/**
 * Открывает документ строки.
 *
 * Вид документа определяет, за чем идти: смена уже прочитана вместе
 * со списком, остальное кабинет отдаёт по идентификатору операции.
 * Отказ кабинета возвращает `null` — причина уже показана общей
 * строкой помехи, и закрывать ею список нечем.
 */
suspend fun openDocument(
    cabinet: CabinetSession,
    registerId: String?,
    kind: DocumentKind,
    target: RowTarget?
): OpenedDocument? {
    if (target == null) return null
    if (target is RowTarget.Local) return OpenedDocument.Shift(target.shift)
    val token = cabinet.token ?: return null
    val id = registerId ?: return null
    val transaction = (target as RowTarget.Remote).transactionId
    return when (kind) {
        DocumentKind.Receipts -> cabinet.guard { cabinet.client.receipt(token, id, transaction) }
            ?.let(OpenedDocument::Receipt)

        DocumentKind.Reports -> cabinet.guard { cabinet.client.report(token, id, transaction) }
            ?.let(OpenedDocument::Report)

        DocumentKind.CashMovements -> cabinet.guard { cabinet.client.cashMovement(token, id, transaction) }
            ?.let(OpenedDocument::Movement)

        DocumentKind.Shifts -> null
    }
}
