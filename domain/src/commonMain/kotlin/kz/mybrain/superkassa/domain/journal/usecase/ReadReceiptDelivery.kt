package kz.mybrain.superkassa.domain.journal.usecase

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import kz.mybrain.superkassa.domain.journal.port.ReceiptDeliveries
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.answerSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Что с доставкой чека покупателю по каждому каналу — пином работающего. */
class ReadReceiptDelivery(private val deliveries: ReceiptDeliveries, private val signed: SignedKkm) {

    /** @return по записи на канал; пусто — доставка не заказывалась. */
    suspend operator fun invoke(documentId: String): Answer<List<ReceiptDeliveryResponse>> =
        signed.answerSeated { seat -> deliveries.of(seat.kkmId, documentId, seat.pin) }
}
