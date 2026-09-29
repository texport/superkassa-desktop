package kz.mybrain.superkassa.integrations.bfdcabinet.documents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/**
 * X- или Z-отчёт в списке.
 *
 * @property type вид отчёта (`reportType`).
 * @property total продажи смены (`saleTotal`).
 */
@Serializable
data class CabinetReport(
    val transactionId: String,
    @SerialName("reportType") val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    @SerialName("saleTotal") val total: CabinetDecimal? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null
)

/**
 * Отчёт целиком: смена, итоги и состояние доставки.
 *
 * @property cashBalance наличные в ящике на момент отчёта.
 * @property kkmDocumentNumber номер документа кассы (`protocolDocumentId`).
 * @property deliveryStatus итог в КГД: `SENT`, `DELIVERED`, `REJECTED`, `FAILED`; пусто — итога нет.
 * @property deliveryMessage почему КГД отклонил отчёт; пусто — не отклонял.
 * @property sdfRequestId номер запроса к службе передачи в КГД — для поддержки.
 */
@Serializable
data class CabinetReportDetails(
    val transactionId: String,
    @SerialName("reportType") val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    @SerialName("saleTotal") val total: CabinetDecimal? = null,
    val returnTotal: CabinetDecimal? = null,
    val buyTotal: CabinetDecimal? = null,
    val buyReturnTotal: CabinetDecimal? = null,
    val cashBalance: CabinetDecimal? = null,
    val receiptsCount: Int? = null,
    @SerialName("protocolDocumentId") val kkmDocumentNumber: String? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    val deliveryResultAt: String? = null,
    val deliveryMessage: String? = null,
    val sdfRequestId: String? = null,
    override val payload: JsonElement? = null
) : ProtocolDocument

/**
 * Внесение или изъятие в списке.
 *
 * @property type вид движения (`movementType`).
 */
@Serializable
data class CabinetCashMovement(
    val transactionId: String,
    @SerialName("movementType") val type: String? = null,
    val amount: CabinetDecimal? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null,
    val sendStatus: String? = null
)

/** Внесение или изъятие целиком: сумма, смена и состояние передачи. */
@Serializable
data class CabinetCashMovementDetails(
    val transactionId: String,
    @SerialName("movementType") val type: String? = null,
    val amount: CabinetDecimal? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val protocolDocumentId: String? = null,
    val sendStatus: String? = null,
    override val payload: JsonElement? = null
) : ProtocolDocument
