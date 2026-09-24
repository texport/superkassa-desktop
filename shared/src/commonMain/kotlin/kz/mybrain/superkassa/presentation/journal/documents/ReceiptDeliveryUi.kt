package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import kz.mybrain.superkassa.domain.journal.model.ReceiptDeliveryRules

/**
 * Доставка открытого в журнале чека покупателю — по каналам.
 *
 * @property documentId чей это ответ: пришедший за другим чеком не показывается.
 * @property number номер чека, как он стоит в строке журнала.
 * @property awaitingBfd чек ещё в очереди БФД: доставки пока нет, и это не отказ.
 * @property deliveries по записи на канал и вид отправки.
 * @property reading касса ещё не ответила.
 * @property sending идёт повтор: вторую кнопку не нажать.
 * @property problem почему касса не ответила или отказала — словами кассира.
 */
data class ReceiptDeliveryUi(
    val documentId: String,
    val number: String,
    val awaitingBfd: Boolean,
    val deliveries: List<ReceiptDeliveryResponse> = emptyList(),
    val reading: Boolean = true,
    val sending: Boolean = false,
    val problem: String? = null
) {
    /** Есть окончательно не удавшаяся доставка: повтор — главное действие окна. */
    val resendable: Boolean get() = ReceiptDeliveryRules.resendable(deliveries)

    /** Кнопку повтора можно нажать сейчас. */
    val canResend: Boolean get() = resendable && !reading && !sending
}
