package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

/**
 * Чек в кабинете: строка списка, отбор списка и чек целиком.
 *
 * Отдельный предмет, потому что чек — единственный документ с позициями,
 * оплатами и отбором по сумме; у отчёта и движения денег нет ни того,
 * ни другого, и общий файл заставлял бы читать их поля заодно.
 */

/** Чек в списке. */
@Serializable
data class CabinetReceipt(
    val transactionId: String,
    val receiptNumber: String? = null,
    val shiftNumber: Int? = null,
    val operationType: String? = null,
    @Contextual val total: BigDecimal? = null,
    val createdAt: String? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    val kgdMark: String? = null,
    val kgdMarkAt: String? = null
)

/**
 * Отбор чеков.
 *
 * Границы периода — строками ISO-8601 в UTC: кабинет разбирает их
 * в момент времени сам, а своего представления времени у отбора нет.
 */
@Serializable
data class ReceiptSearch(
    val page: Int = 0,
    val size: Int = PAGE_SIZE,
    val receiptNumber: String? = null,
    val shiftNumber: Int? = null,
    @Contextual val sumFrom: BigDecimal? = null,
    @Contextual val sumTo: BigDecimal? = null,
    val operationTypes: List<String>? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null
)

/**
 * Чек целиком, как его принял ОФД.
 *
 * Поля, которых не было в сообщении кассы, равны `null`: ноль означает
 * присланный ноль, а не отсутствие — на этом различии держится разбор
 * расхождений с кассой.
 *
 * Оплаты кабинет отдаёт двумя итогами — наличными и картой, — а налог чека
 * одной суммой; экран получает их теми же списками, что и позиции.
 */
@Serializable
data class CabinetReceiptDetails(
    val transactionId: String,
    val receiptNumber: String? = null,
    @SerialName("protocolDocumentId") val kkmDocumentNumber: String? = null,
    val shiftNumber: Int? = null,
    val operationType: String? = null,
    @Contextual val total: BigDecimal? = null,
    @Contextual val taxTotal: BigDecimal? = null,
    @Contextual val cashTotal: BigDecimal? = null,
    @Contextual val cardTotal: BigDecimal? = null,
    val createdAt: String? = null,
    val registrationNumber: String? = null,
    val operator: DocumentOperator? = null,
    val items: List<DocumentItem> = emptyList(),
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    val sentAt: String? = null,
    val deliveryResultAt: String? = null,
    val kgdMark: String? = null,
    val kgdMarkAt: String? = null,
    override val payload: JsonElement? = null
) : ProtocolDocument {
    val payments: List<DocumentPayment>
        get() = listOfNotNull(
            cashTotal?.takeIf { it.signum() != 0 }?.let { DocumentPayment("CASH", it) },
            cardTotal?.takeIf { it.signum() != 0 }?.let { DocumentPayment("CARD", it) }
        )

    val taxes: List<DocumentTax>
        get() = listOfNotNull(taxTotal?.takeIf { it.signum() != 0 }?.let { DocumentTax(type = "VAT", sum = it) })

    val amounts: DocumentAmounts? get() = total?.let { DocumentAmounts(total = it) }
}
