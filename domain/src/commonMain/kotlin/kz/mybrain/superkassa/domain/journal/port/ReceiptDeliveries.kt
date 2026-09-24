package kz.mybrain.superkassa.domain.journal.port

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse

/**
 * Доставка чека покупателю — SMS, мессенджеры, почта, сетевой принтер.
 *
 * Доставку ведёт ядро: задачи ставятся, когда БФД принял чек, и уходят
 * в фоне. Журналу нужно от неё ровно два вопроса — что с чеком сейчас
 * по каждому каналу и «отправить ещё раз». Получателя ответ не несёт:
 * это персональные данные покупателя.
 *
 * Отказ ядра по существу приходит его исключением `SuperkassaException`
 * с кодом и словами на трёх языках.
 */
interface ReceiptDeliveries {

    /**
     * Доставка документа [documentId] по каналам.
     *
     * @return по записи на канал; пусто — доставка не заказывалась.
     */
    suspend fun of(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse>

    /**
     * Повторяет доставку, окончательно не удавшуюся, и отвечает, что вышло.
     *
     * Доставленное второй раз не уходит; ждущее повтора уйдёт в свой срок.
     */
    suspend fun resend(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse>
}
