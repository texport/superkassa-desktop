package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCashMovementDetails
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceiptDetails
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReportDetails
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovement
import kz.mybrain.superkassa.desktop.server.cabinet.receipt
import kz.mybrain.superkassa.desktop.server.cabinet.report
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Строка документа на экране.
 *
 * Состояние справа выбирается по тому, что за запись: у чека и отчёта
 * это доставка в ОФД, у смены — открыта она или закрыта. Прежде и то
 * и другое приписывалось словами в конец второй строки и терялось.
 */
@Composable
fun RecordRowOf(row: DocumentRow, texts: CabinetTexts, striped: Boolean, onOpen: () -> Unit) {
    RecordRow(
        title = row.title,
        subtitle = row.subtitle,
        amount = row.amount,
        striped = striped,
        onClick = onOpen.takeIf { row.target != null },
        trailing = {
            when {
                row.delivery != null -> DeliveryChip(row.delivery, texts)
                row.state != null -> CabinetStatusChip(row.state, texts)
                else -> Unit
            }
        }
    )
}

/**
 * Раскрытый документ.
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
    row: DocumentRow
): OpenedDocument? {
    val target = row.target ?: return null
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
