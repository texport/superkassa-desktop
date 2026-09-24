package kz.mybrain.superkassa.domain.cabinet.model.documents

/**
 * Виды документов кассы в кабинете.
 *
 * Выбор вида решает, какой список читать у кабинета, — это источник
 * строк, а не отбор уже прочитанного.
 *
 * @param dated отбирает ли кабинет этот вид по сроку: смен по сроку он не отдаёт.
 * @param countIn сколько таких документов у кассы за всё время: по этому
 *   числу пустой список отличает «этого у кассы нет» от «нет за срок».
 */
enum class DocumentKind(val countIn: (DocumentsOverview) -> Long, val dated: Boolean = true) {
    Receipts({ it.receiptsCount }),
    Shifts({ it.shiftsCount }, dated = false),
    Reports({ it.reportsCount }),
    CashMovements({ it.cashMovementsCount })
}

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

/**
 * Раскрытый документ кабинета.
 *
 * Раскрытым бывает любой вид, а не только чек: отчёт объясняет итог смены,
 * движение денег — кто и когда взял из ящика.
 */
sealed interface OpenedDocument {
    data class Receipt(val details: CabinetReceiptDetails) : OpenedDocument
    data class Report(val details: CabinetReportDetails) : OpenedDocument
    data class Movement(val details: CabinetCashMovementDetails) : OpenedDocument
    data class Shift(val shift: CabinetShift) : OpenedDocument
}

/**
 * Пакет протокола документа: запрос кассы и ответ ОФД. Своего вида
 * документа у кабинета нет: форму рисует касса по этому пакету.
 */
val OpenedDocument.packet: String?
    get() = when (this) {
        is OpenedDocument.Receipt -> details.packet
        is OpenedDocument.Report -> details.packet
        is OpenedDocument.Movement -> details.packet
        is OpenedDocument.Shift -> null
    }
