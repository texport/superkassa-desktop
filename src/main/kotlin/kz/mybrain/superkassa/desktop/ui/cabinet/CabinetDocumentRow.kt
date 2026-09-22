package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCashMovement
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceipt
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReport
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.ui.history.JournalDelivery
import kz.mybrain.superkassa.desktop.ui.history.JournalEntry
import kz.mybrain.superkassa.desktop.ui.history.JournalState
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.time.Instant

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
internal fun receiptRow(receipt: CabinetReceipt, texts: CabinetTexts): CabinetDocumentRow =
    CabinetDocumentRow(
        entry = JournalEntry(
            key = receipt.transactionId,
            at = cabinetMillis(receipt.createdAt),
            moment = cabinetMoment(receipt.createdAt),
            typeCode = receipt.operationType,
            type = documentTitle(receipt.operationType, texts),
            number = receipt.receiptNumber ?: Glyphs.DASH,
            numberOrder = receipt.receiptNumber?.toLongOrNull(),
            amount = cabinetSum(receipt.total),
            amountOrder = receipt.total,
            // В столбце признака стоит отметка КГД: своего фискального
            // признака кабинет в списке не отдаёт, а отметка — то же
            // по смыслу, чем государство помечает принятый документ,
            // и по ней владелец сверяет чек с покупателем.
            sign = receipt.kgdMark ?: Glyphs.DASH,
            delivery = cabinetState(receipt.deliveryStatus, receipt.sendStatus),
            shiftNo = receipt.shiftNumber?.toLong(),
            about = receipt.kgdMark?.let { texts.kgdMarked }.orEmpty(),
            printable = drawable(cabinetState(receipt.deliveryStatus, receipt.sendStatus))
        ),
        target = RowTarget.Remote(receipt.transactionId)
    )

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
            moment = cabinetMoment(shift.openedAt),
            typeCode = null,
            type = texts.shift,
            number = Glyphs.DASH,
            numberOrder = null,
            amount = cabinetSum(shift.total),
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

/** Отчёт: X или Z, смена и её итог. */
internal fun reportRow(report: CabinetReport, texts: CabinetTexts): CabinetDocumentRow =
    CabinetDocumentRow(
        entry = JournalEntry(
            key = report.transactionId,
            at = cabinetMillis(report.createdAt),
            moment = cabinetMoment(report.createdAt),
            typeCode = report.type,
            type = documentTitle(report.type, texts),
            number = Glyphs.DASH,
            numberOrder = null,
            amount = cabinetSum(report.total),
            amountOrder = report.total,
            sign = Glyphs.DASH,
            delivery = cabinetState(report.deliveryStatus, report.sendStatus),
            shiftNo = report.shiftNumber?.toLong(),
            printable = drawable(cabinetState(report.deliveryStatus, report.sendStatus))
        ),
        target = RowTarget.Remote(report.transactionId)
    )

/** Внесение или изъятие денег из ящика. */
internal fun movementRow(movement: CabinetCashMovement, texts: CabinetTexts): CabinetDocumentRow =
    CabinetDocumentRow(
        entry = JournalEntry(
            key = movement.transactionId,
            at = cabinetMillis(movement.createdAt),
            moment = cabinetMoment(movement.createdAt),
            typeCode = movement.type,
            type = documentTitle(movement.type, texts),
            number = Glyphs.DASH,
            numberOrder = null,
            amount = cabinetSum(movement.amount),
            amountOrder = movement.amount,
            sign = Glyphs.DASH,
            delivery = cabinetState(null, movement.sendStatus),
            shiftNo = movement.shiftNumber?.toLong(),
            printable = drawable(cabinetState(null, movement.sendStatus))
        ),
        target = RowTarget.Remote(movement.transactionId)
    )

/**
 * Есть ли у документа кабинета печатная форма.
 *
 * Отвергнутый БФД документ фискальным не стал: его нет ни в БФД,
 * ни в отчётности. Печатная форма при этом выглядит как настоящий чек —
 * с номером, признаком и QR-кодом, — и покупатель принимает её
 * за подтверждение покупки.
 *
 * Журнал кассы так и решает про свои документы; кабинет заполняет
 * ту же таблицу, и мера у обоих одна. Чек, пробитый без связи, сюда
 * не попадает: он фискальный, просто ещё не доставлен.
 */
internal fun drawable(delivery: JournalDelivery?): Boolean = delivery != JournalDelivery.Refused

/**
 * Состояние доставки кабинета словами журнала.
 *
 * Кабинет отдаёт коды своего сервера — `ONLINE_OK`, `DELIVERY_ERROR`, —
 * и узел называет то же самое иначе. Обоим состояние приводится к одному
 * перечислению, иначе отбор «отклонённые» на двух экранах находил бы
 * разное.
 */
internal fun cabinetDelivery(code: String?): JournalDelivery? = when {
    code.isNullOrBlank() -> null
    REFUSED.any { code.contains(it, ignoreCase = true) } -> JournalDelivery.Refused
    // `ACCEPTED` — слово ближнего плеча: сервис приёма документ принял.
    // Без него состояние съезжало в «ждёт отправки», хотя ждать уже нечего.
    code.contains("OK", ignoreCase = true) ||
        code.equals("DELIVERED", ignoreCase = true) ||
        code.equals("ACCEPTED", ignoreCase = true) -> JournalDelivery.Delivered

    else -> JournalDelivery.Queued
}

/**
 * Состояние документа в кабинете по двум плечам передачи.
 *
 * Дальнее плечо — доставка в органы госдоходов, ближнее — приём
 * сервисом приёма. Пока службы передачи в контуре нет, дальнее пусто
 * у каждого документа, и столбец состояния выглядел сломанным: пустая
 * клетка на всю страницу. Ближнее известно всегда, и оно отвечает
 * на вопрос владельца «ушло ли из кассы».
 *
 * @param delivery состояние доставки в КГД, как его отдаёт кабинет.
 * @param send состояние приёма сервисом приёма.
 */
internal fun cabinetState(delivery: String?, send: String?): JournalDelivery? =
    cabinetDelivery(delivery) ?: cabinetDelivery(send)

/**
 * Момент документа числом.
 *
 * Кабинет отдаёт время строкой ISO-8601 в UTC; журнал сортирует строки
 * по числу — по тексту «2026-09-07T19:42» и «2026-10-01T08:00» сравнились
 * бы верно, а «07.09.2026» и «01.10.2026» из показа — уже нет.
 */
internal fun cabinetMillis(iso: String?): Long? {
    val value = iso?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
}

/** Начало ключа строки смены: своего идентификатора у неё нет. */
private const val SHIFT_KEY = "shift-"

/** Закрытая смена: только у неё есть Z-отчёт. */
private const val CLOSED = "CLOSED"

/** Хвосты кодов, означающих отказ доставки. */
private val REFUSED = listOf("FAIL", "ERROR", "REJECT")
