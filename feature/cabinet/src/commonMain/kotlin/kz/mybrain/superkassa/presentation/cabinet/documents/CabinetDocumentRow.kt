package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovement
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReport
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.domain.cabinet.model.documents.KgdDelivery
import kz.mybrain.superkassa.domain.cabinet.model.documents.RowTarget
import kz.mybrain.superkassa.presentation.common.document.JournalEntry
import kz.mybrain.superkassa.presentation.common.document.JournalState
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.words.cabinet.statusTitle
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Документы кабинета строками общего журнала.
 *
 * Кабинет — второй источник журнала; первый, узел, приводит свои
 * документы к тем же строкам. Дальше показ, поиск, отбор и порядок строк
 * одинаковы, и чек, найденный кассиром у кассы, владелец находит
 * в кабинете тем же поиском.
 *
 * Печатной формы кабинет не отдаёт вовсе: чеки, отчёты и движение денег
 * он хранит запросом и ответом протокола, а не лентой. Этим же пакетом
 * форму рисует узел — тем же рисовальщиком, что и свои документы.
 */
data class CabinetDocumentRow(val entry: JournalEntry, val target: RowTarget?)

/** Чек: вид операции, номер, сумма и отметка КГД. */
fun receiptRow(receipt: CabinetReceipt, texts: CabinetTexts): CabinetDocumentRow {
    val kgd = KgdDelivery.ofReceipt(receipt.deliveryStatus, receipt.sendStatus)
    return CabinetDocumentRow(
        entry = JournalEntry(
            key = receipt.transactionId,
            at = cabinetMillis(receipt.createdAt),
            moment = Dates.momentOf(receipt.createdAt),
            typeCode = receipt.operationType,
            type = documentTitle(receipt.operationType, texts),
            number = receipt.receiptNumber ?: Glyphs.DASH,
            numberOrder = receipt.receiptNumber?.toLongOrNull(),
            amount = Money.format(receipt.total),
            amountOrder = receipt.total,
            // В столбце признака стоит отметка КГД: своего фискального
            // признака кабинет в списке не отдаёт, а отметка — то же
            // по смыслу, чем государство помечает принятый документ,
            // и по ней владелец сверяет чек с покупателем.
            sign = receipt.kgdMark ?: Glyphs.DASH,
            delivery = kgd.journal(),
            shiftNo = receipt.shiftNumber?.toLong(),
            about = receipt.kgdMark?.let { texts.documents.kgdMarked }.orEmpty(),
            deliveryWords = kgd.words(texts),
            printable = kgd.drawable()
        ),
        target = RowTarget.Remote(receipt.transactionId)
    )
}

/**
 * Смена.
 *
 * Отдельной ручки у смены нет: всё, что о ней известно, пришло вместе
 * со списком — поэтому и раскрывается она из прочитанного, а не повторным
 * обращением.
 *
 * Печатной формы у смены нет и не бывает: смена — не документ, а срок
 * между открытием и закрытием. Печатается её Z-отчёт, и он стоит своей
 * строкой в «Отчётах». Кнопка, которой нечего напечатать, хуже её
 * отсутствия.
 */
internal fun shiftRow(shift: CabinetShift, texts: CabinetTexts): CabinetDocumentRow =
    CabinetDocumentRow(
        entry = JournalEntry(
            key = "$SHIFT_KEY${shift.shiftNumber}",
            at = cabinetMillis(shift.openedAt),
            moment = Dates.momentOf(shift.openedAt),
            typeCode = null,
            type = texts.register.shift,
            number = Glyphs.DASH,
            numberOrder = null,
            amount = Money.format(shift.total),
            amountOrder = shift.total,
            sign = Glyphs.DASH,
            // О доставке смены говорить нечего: она открыта или закрыта.
            delivery = null,
            shiftNo = shift.shiftNumber.toLong(),
            state = shift.state?.let { JournalState(statusTitle(it, texts), it == CLOSED) },
            printable = false
        ),
        target = RowTarget.Local(shift)
    )

/** Отчёт: X или Z, смена и её итог; X-отчёт в КГД не передаётся. */
internal fun reportRow(report: CabinetReport, texts: CabinetTexts): CabinetDocumentRow {
    val kgd = KgdDelivery.ofReport(report.type, report.deliveryStatus, report.sendStatus)
    return CabinetDocumentRow(
        entry = JournalEntry(
            key = report.transactionId,
            at = cabinetMillis(report.createdAt),
            moment = Dates.momentOf(report.createdAt),
            typeCode = report.type,
            type = documentTitle(report.type, texts),
            number = Glyphs.DASH,
            numberOrder = null,
            amount = Money.format(report.total),
            amountOrder = report.total,
            sign = Glyphs.DASH,
            delivery = kgd.journal(),
            shiftNo = report.shiftNumber?.toLong(),
            deliveryWords = kgd.words(texts),
            printable = kgd.drawable()
        ),
        target = RowTarget.Remote(report.transactionId)
    )
}

/** Внесение или изъятие денег из ящика: в КГД не передаётся. */
internal fun movementRow(movement: CabinetCashMovement, texts: CabinetTexts): CabinetDocumentRow {
    val kgd = KgdDelivery.ofMovement()
    return CabinetDocumentRow(
        entry = JournalEntry(
            key = movement.transactionId,
            at = cabinetMillis(movement.createdAt),
            moment = Dates.momentOf(movement.createdAt),
            typeCode = movement.type,
            type = documentTitle(movement.type, texts),
            number = Glyphs.DASH,
            numberOrder = null,
            amount = Money.format(movement.amount),
            amountOrder = movement.amount,
            sign = Glyphs.DASH,
            delivery = kgd.journal(),
            shiftNo = movement.shiftNumber?.toLong(),
            deliveryWords = kgd.words(texts),
            printable = kgd.drawable()
        ),
        target = RowTarget.Remote(movement.transactionId)
    )
}

/** Начало ключа строки смены: своего идентификатора у неё нет. */
private const val SHIFT_KEY = "shift-"

/** Закрытая смена: только у неё есть Z-отчёт. */
private const val CLOSED = "CLOSED"
