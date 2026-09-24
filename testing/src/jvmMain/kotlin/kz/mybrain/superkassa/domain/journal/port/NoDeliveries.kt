package kz.mybrain.superkassa.domain.journal.port

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse

/** Доставка, которую ни один чек не заказывал: проверкам без неё она пуста. */
object NoDeliveries : ReceiptDeliveries {
    override suspend fun of(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse> =
        emptyList()

    override suspend fun resend(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse> =
        emptyList()
}
