package kz.mybrain.superkassa.data.kassa.delivery

import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.journal.port.ReceiptDeliveries

/**
 * Доставка чека кассы, поднятой в процессе приложения.
 *
 * Фасад доставки блокирующий — повтор ждёт ответа канала, — поэтому
 * вызов уходит в [io], как и у кассы.
 */
class EmbeddedDeliveries(
    private val api: DeliveryApi,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ReceiptDeliveries {

    override suspend fun of(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse> =
        withContext(io) { api.receiptDeliveries(kkmId, documentId, pin) }

    override suspend fun resend(kkmId: String, documentId: String, pin: String): List<ReceiptDeliveryResponse> =
        withContext(io) { api.resendReceipt(kkmId, documentId, pin) }
}
