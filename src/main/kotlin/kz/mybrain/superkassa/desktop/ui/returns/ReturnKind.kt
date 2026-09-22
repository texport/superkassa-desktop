package kz.mybrain.superkassa.desktop.ui.returns

import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.ReturnStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleStrings

/**
 * Направление возврата.
 *
 * Возврат продажи выдаёт деньги покупателю, возврат покупки — принимает
 * их обратно в кассу. Путать эти два значит разойтись в денежном ящике.
 */
enum class ReturnKind(
    val title: (ReturnStrings) -> String,
    /** Одно слово для сегмента: «Возврат» стоит заголовком рядом. */
    val shortTitle: (SaleStrings) -> String,
    val action: (ReturnStrings) -> String,
    /** Тип документа, который может быть основанием такого возврата. */
    val basisType: String,
    val emptyText: (ReturnJournalTexts) -> String
) {
    Sell({ it.saleReturn }, { it.sale }, { it.giveBack }, "SALE", { it.noSaleBasis }),
    Buy({ it.purchaseReturn }, { it.purchase }, { it.takeBack }, "BUY", { it.noBuyBasis });

    /**
     * Чеки смены, годные в основание такого возврата.
     *
     * Отбор строгий по типу: возврат по возврату протокол не допускает,
     * поэтому RETURN и BUY_RETURN сюда не попадают ни при каком условии.
     * Старый тип «CHECK» здесь тоже не принимается: он не различал продажу
     * и покупку, и принять его значило бы предложить кассиру вернуть
     * покупку как продажу. В открытой смене таких записей и не бывает —
     * их пишет только прежняя версия узла.
     *
     * Документ без номера или без суммы отброшен: чек-основание требует
     * и то, и другое, а без них кнопка возврата раньше просто молчала.
     *
     * Отвергнутый ОФД чек отброшен тоже: фискальным он не стал, в ОФД его
     * нет, и возврат по нему ОФД отвергнет следом. Кассир видел такой чек
     * в списке наравне с проведёнными и, выбрав его, отдавал деньги
     * покупателю под чек возврата, которого у ОФД не будет.
     */
    fun basisIn(documents: List<Document>): List<Document> =
        documents.filter {
            it.docType == basisType && it.docNo != null && it.totalAmount != null && !it.refusedByOfd
        }
}
