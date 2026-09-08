package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentPeriod
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
 * @param target что откроется по нажатию.
 */
data class DocumentRow(
    val title: String,
    val subtitle: String,
    val amount: String? = null,
    val delivery: String? = null,
    val state: String? = null,
    val target: RowTarget? = null
)

/**
 * Что стоит за строкой списка.
 *
 * У чека, отчёта и движения денег кабинет отдаёт содержимое отдельной
 * ручкой по идентификатору операции. У смены такой ручки нет: всё, что
 * о ней известно, приходит вместе со списком — поэтому смена и открывается
 * из того, что уже прочитано, а не повторным обращением.
 */
sealed interface RowTarget {
    data class Remote(val transactionId: String) : RowTarget
    data class Local(val shift: CabinetShift) : RowTarget
}

/** Прочитанная страница списка и сколько всего строк за сроком. */
data class DocumentSlice(val rows: List<DocumentRow> = emptyList(), val total: Long = 0)

/**
 * Читает страницу выбранного вида документов.
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
    page: Int,
    period: DocumentPeriod,
    texts: CabinetTexts
): DocumentSlice = when (kind) {
    DocumentKind.Receipts -> receiptRows(cabinet, token, id, page, period, texts)
    DocumentKind.Shifts -> shiftRows(cabinet, token, id, page, texts)
    DocumentKind.Reports -> reportRows(cabinet, token, id, page, period, texts)
    DocumentKind.CashMovements -> movementRows(cabinet, token, id, page, period, texts)
}

private suspend fun receiptRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    page: Int,
    period: DocumentPeriod,
    texts: CabinetTexts
): DocumentSlice {
    val found = cabinet.client.receipts(
        token,
        id,
        ReceiptSearch(page = page, dateFrom = period.fromText(), dateTo = period.toText())
    )
    return DocumentSlice(
        rows = found.items.map { receipt ->
            DocumentRow(
                title = listOfNotNull(documentTitle(receipt.operationType, texts), receipt.receiptNumber)
                    .joinToString(" · "),
                subtitle = listOfNotNull(
                    cabinetMoment(receipt.createdAt),
                    receipt.kgdMark?.let { texts.kgdMarked }
                ).joinToString(" · "),
                amount = cabinetSum(receipt.total),
                delivery = receipt.deliveryStatus,
                target = RowTarget.Remote(receipt.transactionId)
            )
        },
        total = found.totalElements
    )
}

private suspend fun shiftRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    page: Int,
    texts: CabinetTexts
): DocumentSlice {
    val found = cabinet.client.shifts(token, id, page)
    return DocumentSlice(
        rows = found.items.map { shift ->
            DocumentRow(
                title = "${texts.shift} ${shift.shiftNumber}",
                subtitle = listOf(cabinetMoment(shift.openedAt), cabinetMoment(shift.closedAt))
                    .joinToString(" — "),
                amount = cabinetSum(shift.total),
                state = shift.state,
                target = RowTarget.Local(shift)
            )
        },
        total = found.totalElements
    )
}

private suspend fun reportRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    page: Int,
    period: DocumentPeriod,
    texts: CabinetTexts
): DocumentSlice {
    val found = cabinet.client.reports(token, id, page, period)
    return DocumentSlice(
        rows = found.items.map { report ->
            DocumentRow(
                title = listOfNotNull(
                    documentTitle(report.type, texts),
                    report.shiftNumber?.let { "${texts.shift} $it" }
                ).joinToString(" · "),
                subtitle = cabinetMoment(report.createdAt),
                amount = cabinetSum(report.total),
                delivery = report.deliveryStatus,
                target = RowTarget.Remote(report.transactionId)
            )
        },
        total = found.totalElements
    )
}

private suspend fun movementRows(
    cabinet: CabinetSession,
    token: String,
    id: String,
    page: Int,
    period: DocumentPeriod,
    texts: CabinetTexts
): DocumentSlice {
    val found = cabinet.client.cashMovements(token, id, page, period)
    return DocumentSlice(
        rows = found.items.map { movement ->
            DocumentRow(
                title = documentTitle(movement.type, texts),
                subtitle = listOfNotNull(
                    cabinetMoment(movement.createdAt),
                    movement.shiftNumber?.let { "${texts.shift} $it" }
                ).joinToString(" · "),
                amount = cabinetSum(movement.amount),
                target = RowTarget.Remote(movement.transactionId)
            )
        },
        total = found.totalElements
    )
}

/**
 * Виды документов кассы в кабинете.
 *
 * @param dated отбирает ли кабинет этот вид по сроку.
 */
enum class DocumentKind(val title: (CabinetTexts) -> String, val dated: Boolean = true) {
    Receipts({ it.receipts }),
    Shifts({ it.shifts }, dated = false),
    Reports({ it.reports }),
    CashMovements({ it.cashMovements })
}
