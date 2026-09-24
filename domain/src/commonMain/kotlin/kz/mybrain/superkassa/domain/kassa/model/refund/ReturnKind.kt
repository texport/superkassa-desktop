package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptOperationType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.document.model.refusedByOfd

/**
 * Направление возврата.
 *
 * Возврат продажи выдаёт деньги покупателю, возврат покупки — принимает
 * их обратно в кассу. Путать эти два значит разойтись в денежном ящике.
 * Названия для кассира — в надписях области.
 */
enum class ReturnKind(
    /** Тип документа, который может быть основанием такого возврата. */
    val basisType: String,
    /** Операция чека возврата у кассы. */
    val operation: ReceiptOperationType
) {
    Sell("SALE", ReceiptOperationType.SELL_RETURN),
    Buy("BUY", ReceiptOperationType.BUY_RETURN);

    /**
     * Чеки смены, годные в основание такого возврата.
     *
     * Отбор строгий по типу: возврат по возврату протокол не допускает,
     * поэтому RETURN и BUY_RETURN сюда не попадают ни при каком условии.
     * Старый тип «CHECK» здесь тоже не принимается: он не различал продажу
     * и покупку, и принять его значило бы предложить кассиру вернуть
     * покупку как продажу. В открытой смене таких записей и не бывает —
     * их писала только прежняя версия узла.
     *
     * Документ без номера или без суммы отброшен: чек-основание требует
     * и то, и другое, а без них кнопка возврата раньше просто молчала.
     *
     * Отвергнутый ОФД чек отброшен тоже: фискальным он не стал, в ОФД его
     * нет, и возврат по нему ОФД отвергнет следом. Кассир видел такой чек
     * в списке наравне с проведёнными и, выбрав его, отдавал деньги
     * покупателю под чек возврата, которого у ОФД не будет.
     */
    fun basisIn(documents: List<FiscalDocumentResponse>): List<FiscalDocumentResponse> =
        documents.filter {
            it.docType == basisType && it.docNo != null && it.totalAmount != null && !it.refusedByOfd
        }
}

/**
 * Подходит ли документ под набранный номер.
 *
 * Совпадение по вхождению, а не по началу: кассир набирает последние
 * цифры с чека, не переписывая номер целиком. Искать можно и по номеру
 * чека, и по фискальному признаку: на чеке покупателя стоят оба.
 */
fun FiscalDocumentResponse.matches(typed: String): Boolean {
    val wanted = typed.trim()
    if (wanted.isEmpty()) return true
    return listOfNotNull(number?.toString(), fiscalSign, autonomousSign).any { it.contains(wanted) }
}

/** Сутки кассира по часам рабочего места: от полуночи до полуночи, в миллисекундах. */
fun LocalDate.span(zone: TimeZone = TimeZone.currentSystemDefault()): Pair<Long, Long> =
    atStartOfDayIn(zone).toEpochMilliseconds() to plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
