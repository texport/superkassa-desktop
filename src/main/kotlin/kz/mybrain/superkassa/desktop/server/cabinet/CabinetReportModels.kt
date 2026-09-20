package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

/**
 * X- и Z-отчёт в кабинете: строка списка и отчёт целиком.
 *
 * Отдельный предмет от чека: у отчёта нет ни позиций, ни оплат, зато есть
 * счёт чеков смены и её итоги. Поля с одинаковыми именами — сумма, смена,
 * состояние доставки — совпадают случайно, и общий тип на двоих однажды
 * заставил бы чек нести пустой счёт чеков.
 */

/** Отчёт в списке. */
@Serializable
data class CabinetReport(
    val transactionId: String,
    @SerialName("reportType") val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    @SerialName("saleTotal") @Contextual val total: BigDecimal? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null
)

/** Отчёт целиком: смена, итоги и состояние доставки. */
@Serializable
data class CabinetReportDetails(
    val transactionId: String,
    @SerialName("reportType") val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    @SerialName("saleTotal") @Contextual val total: BigDecimal? = null,
    @Contextual val returnTotal: BigDecimal? = null,
    /** Покупка у населения за смену по данным отчёта; пусто — такой операции в нём нет. */
    @Contextual val buyTotal: BigDecimal? = null,
    @Contextual val buyReturnTotal: BigDecimal? = null,
    /** Наличные в ящике на момент отчёта; не оплаченное наличными за смену. */
    @Contextual val cashBalance: BigDecimal? = null,
    val receiptsCount: Int? = null,
    @SerialName("protocolDocumentId") val kkmDocumentNumber: String? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    override val payload: JsonElement? = null
) : ProtocolDocument
