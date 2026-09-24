package kz.mybrain.superkassa.domain.journal.usecase

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import kz.mybrain.superkassa.domain.journal.model.ReceiptDeliveryRules
import kz.mybrain.superkassa.domain.journal.port.ReceiptDeliveries
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.answerSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Отправляет чек покупателю ещё раз — по каналам, где доставка не удалась.
 *
 * Повтор просится, только когда есть что повторять: иначе касса ответила
 * бы тем же списком, и кассир решил бы, что чек ушёл заново.
 */
class ResendReceipt(private val deliveries: ReceiptDeliveries, private val signed: SignedKkm) {

    /**
     * @param shown доставка, какой её видит кассир.
     * @return доставка после повтора; `null` — повторять нечего.
     */
    suspend operator fun invoke(
        documentId: String,
        shown: List<ReceiptDeliveryResponse>
    ): Answer<List<ReceiptDeliveryResponse>>? {
        if (!ReceiptDeliveryRules.resendable(shown)) return null
        return signed.answerSeated { seat -> deliveries.resend(seat.kkmId, documentId, seat.pin) }
    }
}
