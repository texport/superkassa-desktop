package kz.mybrain.superkassa.domain.journal.model

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.DocumentType
import kz.mybrain.superkassa.domain.document.model.DeliveryCodes
import kz.mybrain.superkassa.domain.document.model.refusedByOfd

/**
 * Правила доставки чека покупателю.
 *
 * Доставляется только чек — продажа, возврат, покупка и её возврат;
 * отчёт и внесение покупателю не уходят. Отвергнутый БФД чек фискальным
 * не стал, и отправлять покупателю нечего.
 */
object ReceiptDeliveryRules {

    private val receipts = setOf(DocumentType.SALE, DocumentType.RETURN, DocumentType.BUY, DocumentType.BUY_RETURN)
        .map { it.name }
        .toSet()

    /** Документ — чек, который мог уйти покупателю. */
    fun delivers(document: FiscalDocumentResponse): Boolean =
        document.docType in receipts && !document.refusedByOfd

    /**
     * Чек пробит без связи и ждёт очереди: доставка покупателю ставится,
     * когда его примет БФД, и пустой её список значит «ещё рано», а не
     * «не заказывали».
     */
    fun awaitsBfd(document: FiscalDocumentResponse): Boolean = document.ofdStatus in DeliveryCodes.queued

    /**
     * Есть что повторить руками: доставка хотя бы по одному каналу
     * не удалась окончательно.
     *
     * Ждущее повтора касса дошлёт сама в свой срок, а доставленное второй
     * раз не уходит — кнопка над ними обещала бы то, чего не будет.
     */
    fun resendable(deliveries: List<ReceiptDeliveryResponse>): Boolean =
        deliveries.any { it.state == ReceiptDeliveryState.FAILED }
}
