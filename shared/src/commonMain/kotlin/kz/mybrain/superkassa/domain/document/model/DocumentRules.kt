package kz.mybrain.superkassa.domain.document.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse

/**
 * Правила о фискальном документе, каким его отдаёт ядро.
 *
 * Своей копии документа у приложения нет: экраны смены и журнала читают
 * ответ фасада как есть, а вопросы к нему собраны здесь, один раз.
 */

/**
 * Номер, которым документ назван кассиру и покупателю.
 *
 * Один на все экраны и на печатную форму: номер от ОФД не совпадает
 * с бумажным, и разные экраны называли один документ разными числами.
 */
val FiscalDocumentResponse.number: Long?
    get() = printedDocumentNumber ?: docNo

/**
 * ОФД документ не принял.
 *
 * Фискальным чеком он не стал: его нет ни в ОФД, ни в отчётности.
 * Автономный чек сюда не попадает — он фискальный, просто ещё
 * не доставлен.
 */
val FiscalDocumentResponse.refusedByOfd: Boolean
    get() = ofdErrorCode != null || ofdStatus in DeliveryCodes.refused

/**
 * Можно ли показать и напечатать печатную форму.
 *
 * Форма отклонённого документа выглядит как настоящий чек и вводила бы
 * покупателя в заблуждение.
 */
val FiscalDocumentResponse.printable: Boolean
    get() = !refusedByOfd

/**
 * Код отказа, который есть смысл показать кассиру.
 *
 * Коды результата CPCR положительны: ноль — это приём, а `-1` означает
 * отказ на уровне протокола без своего кода. Выдуманное число кассир
 * принял бы за настоящее, и такие значения не показываются.
 */
val FiscalDocumentResponse.refusalCode: Int?
    get() = ofdErrorCode?.takeIf { it > 0 }

/**
 * Есть ли у документа своя сумма.
 *
 * Отчёт и открытие смены суммы не несут: ноль в столбце «Сумма» читается
 * как «не продано ничего», а итоги смены лежат внутри самого отчёта.
 */
val FiscalDocumentResponse.hasOwnAmount: Boolean
    get() = docType !in WITHOUT_AMOUNT

/**
 * Коды доставки, которыми касса отмечает документ.
 *
 * Одно и то же называется двумя наборами: журнал документов отдаёт
 * `SENT`, `FAILED` и `INTERNAL`, а состояние отправки — `ONLINE_OK`,
 * `ONLINE_ERROR`, `OFFLINE_QUEUED` и `NOT_SENT`. Разбираются оба.
 */
object DeliveryCodes {

    /** Документ принят БФД. */
    val delivered: Set<String> = setOf("SENT", "ONLINE_OK")

    /** БФД документ отвергла: фискальным он не стал. */
    val refused: Set<String> = setOf("FAILED", "ONLINE_ERROR")

    /** Документ пробит, а БФД о нём пока не знает. */
    val queued: Set<String> = setOf("PENDING", "OFFLINE_QUEUED", "NOT_SENT")

    /** В БФД не уходит вовсе: такой команды протокол не знает. */
    val internal: Set<String> = setOf("INTERNAL")
}

/** Открытие смены: документ есть, а команды в протоколе нет. */
const val SHIFT_OPEN_DOCUMENT: String = "SHIFT_OPEN"

/** Виды документов, у которых своей суммы не бывает. */
private val WITHOUT_AMOUNT = setOf(SHIFT_OPEN_DOCUMENT, "SHIFT_CLOSE", "X_REPORT", "Z_REPORT")
