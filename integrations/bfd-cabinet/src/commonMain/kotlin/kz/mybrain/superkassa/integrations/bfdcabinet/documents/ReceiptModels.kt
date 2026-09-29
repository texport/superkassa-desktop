package kz.mybrain.superkassa.integrations.bfdcabinet.documents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/** Чек в списке. */
@Serializable
data class CabinetReceipt(
    val transactionId: String,
    val receiptNumber: String? = null,
    val shiftNumber: Int? = null,
    val operationType: String? = null,
    val total: CabinetDecimal? = null,
    val createdAt: String? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    val kgdMark: String? = null,
    val kgdMarkAt: String? = null
)

/**
 * Отбор чеков — телом запроса.
 *
 * Значения по умолчанию в тело не пишутся: кабинет подставляет свои.
 *
 * @property dateFrom начало периода ISO-8601 в UTC.
 * @property dateTo конец периода ISO-8601 в UTC.
 */
@Serializable
data class ReceiptSearch(
    val page: Int = 0,
    val size: Int = CABINET_PAGE_SIZE,
    val receiptNumber: String? = null,
    val shiftNumber: Int? = null,
    val sumFrom: CabinetDecimal? = null,
    val sumTo: CabinetDecimal? = null,
    val operationTypes: List<String>? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null
)

/**
 * Чек целиком, как его принял ОФД.
 *
 * Поля, которых не было в сообщении кассы, равны `null`: ноль означает
 * присланный ноль, а не отсутствие. Оплаты кабинет отдаёт двумя итогами —
 * наличными и картой, — налог чека одной суммой.
 *
 * @property kkmDocumentNumber номер документа кассы (`protocolDocumentId`).
 * @property kgdMark отметка КГД; `null` — чек до КГД не доехал.
 * @property deliveryStatus итог в КГД: `SENT`, `DELIVERED`, `REJECTED`, `FAILED`; пусто — итога нет.
 * @property deliveryMessage почему КГД отклонил чек; пусто — не отклонял.
 * @property sdfRequestId номер запроса к службе передачи в КГД — для поддержки.
 */
@Serializable
data class CabinetReceiptDetails(
    val transactionId: String,
    val receiptNumber: String? = null,
    @SerialName("protocolDocumentId") val kkmDocumentNumber: String? = null,
    val shiftNumber: Int? = null,
    val operationType: String? = null,
    val total: CabinetDecimal? = null,
    val taxTotal: CabinetDecimal? = null,
    val cashTotal: CabinetDecimal? = null,
    val cardTotal: CabinetDecimal? = null,
    val createdAt: String? = null,
    val registrationNumber: String? = null,
    val operator: DocumentOperator? = null,
    val items: List<DocumentItem> = emptyList(),
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    val sentAt: String? = null,
    val deliveryResultAt: String? = null,
    val deliveryMessage: String? = null,
    val sdfRequestId: String? = null,
    val kgdMark: String? = null,
    val kgdMarkAt: String? = null,
    override val payload: JsonElement? = null
) : ProtocolDocument
