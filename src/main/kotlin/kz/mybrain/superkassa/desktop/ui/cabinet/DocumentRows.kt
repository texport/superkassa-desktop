package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.ReceiptSearch
import kz.mybrain.superkassa.desktop.server.cabinet.cashMovements
import kz.mybrain.superkassa.desktop.server.cabinet.receipts
import kz.mybrain.superkassa.desktop.server.cabinet.reports
import kz.mybrain.superkassa.desktop.server.cabinet.shifts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Строка списка документов независимо от его вида.
 *
 * Прежде строка была двумя готовыми предложениями, склеенными точками:
 * «12345 · Продажа · 4 500,00 ₸». Сумма стояла посреди текста и не
 * попадала в столбец, а состояние доставки терялось в хвосте второй
 * строки. Теперь у строки есть части, и показ решает, что где стоит.
 *
 * @param delivery состояние доставки в ОФД — только у того, что туда уходит.
 * @param state состояние самой записи: открыта ли смена.
 */
data class DocumentRow(
    val title: String,
    val subtitle: String,
    val amount: String? = null,
    val delivery: String? = null,
    val state: String? = null,
    val transactionId: String? = null
)

/**
 * Читает выбранный вид документов.
 *
 * Виды разные, а показывается у них одно и то же: чем документ является,
 * на сколько он и что с ним стало. Поэтому список приводится к [DocumentRow]
 * здесь, а не четырьмя похожими разметками на экране.
 */
suspend fun loadDocuments(
    cabinet: CabinetSession,
    token: String,
    id: String,
    kind: DocumentKind,
    texts: CabinetTexts
): List<DocumentRow> = when (kind) {
    DocumentKind.Receipts -> receiptRows(cabinet, token, id, texts)
    DocumentKind.Shifts -> shiftRows(cabinet, token, id, texts)
    DocumentKind.Reports -> reportRows(cabinet, token, id, texts)
    DocumentKind.CashMovements -> movementRows(cabinet, token, id, texts)
}

private suspend fun receiptRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    texts: CabinetTexts
): List<DocumentRow> = cabinet.client.receipts(token, id, ReceiptSearch()).items.map { receipt ->
    DocumentRow(
        title = listOfNotNull(documentTitle(receipt.operationType, texts), receipt.receiptNumber)
            .joinToString(" · "),
        subtitle = listOfNotNull(
            cabinetMoment(receipt.createdAt),
            receipt.kgdMark?.let { texts.kgdMarked }
        ).joinToString(" · "),
        amount = cabinetSum(receipt.total),
        delivery = receipt.deliveryStatus,
        transactionId = receipt.transactionId
    )
}

private suspend fun shiftRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    texts: CabinetTexts
): List<DocumentRow> = cabinet.client.shifts(token, id).items.map { shift ->
    DocumentRow(
        title = "${texts.shift} ${shift.shiftNumber}",
        subtitle = listOf(cabinetMoment(shift.openedAt), cabinetMoment(shift.closedAt))
            .joinToString(" — "),
        amount = cabinetSum(shift.total),
        state = shift.state
    )
}

private suspend fun reportRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    texts: CabinetTexts
): List<DocumentRow> = cabinet.client.reports(token, id).items.map { report ->
    DocumentRow(
        title = listOfNotNull(
            documentTitle(report.type, texts),
            report.shiftNumber?.let { "${texts.shift} $it" }
        ).joinToString(" · "),
        subtitle = cabinetMoment(report.createdAt),
        amount = cabinetSum(report.total),
        delivery = report.deliveryStatus
    )
}

private suspend fun movementRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    texts: CabinetTexts
): List<DocumentRow> = cabinet.client.cashMovements(token, id).items.map { movement ->
    DocumentRow(
        title = documentTitle(movement.type, texts),
        subtitle = listOfNotNull(
            cabinetMoment(movement.createdAt),
            movement.shiftNumber?.let { "${texts.shift} $it" }
        ).joinToString(" · "),
        amount = cabinetSum(movement.amount)
    )
}

/** Виды документов кассы в кабинете. */
enum class DocumentKind(val title: (CabinetTexts) -> String) {
    Receipts({ it.receipts }),
    Shifts({ it.shifts }),
    Reports({ it.reports }),
    CashMovements({ it.cashMovements })
}
