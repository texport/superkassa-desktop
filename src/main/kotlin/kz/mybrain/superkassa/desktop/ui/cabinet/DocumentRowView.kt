package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceiptDetails
import kz.mybrain.superkassa.desktop.server.cabinet.receipt
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
        onClick = onOpen.takeIf { row.transactionId != null },
        trailing = {
            when {
                row.delivery != null -> DeliveryChip(row.delivery, texts)
                row.state != null -> CabinetStatusChip(row.state, texts)
                else -> Unit
            }
        }
    )
}

/** Открывает состав чека; для строк без чека возвращает `null`. */
suspend fun openReceipt(
    cabinet: CabinetSession,
    registerId: String?,
    row: DocumentRow
): CabinetReceiptDetails? {
    val token = cabinet.token ?: return null
    val id = registerId ?: return null
    val transaction = row.transactionId ?: return null
    return cabinet.guard { cabinet.client.receipt(token, id, transaction) }
}
