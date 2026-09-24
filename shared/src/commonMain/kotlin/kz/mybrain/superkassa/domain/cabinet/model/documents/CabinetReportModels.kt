package kz.mybrain.superkassa.domain.cabinet.model.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/**
 * X- и Z-отчёт в кабинете: строка списка и отчёт целиком.
 *
 * Отдельный предмет от чека: у отчёта нет ни позиций, ни оплат, зато есть
 * счёт чеков смены и её итоги. Поля с одинаковыми именами — сумма, смена,
 * состояние доставки — совпадают случайно, и общий тип на двоих однажды
 * заставил бы чек нести пустой счёт чеков.
 */

/** Отчёт в списке. */
data class CabinetReport(
    val transactionId: String,
    val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val total: Decimal? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null
)

/** Отчёт целиком: смена, итоги и состояние доставки. */
data class CabinetReportDetails(
    val transactionId: String,
    val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val total: Decimal? = null,
    val returnTotal: Decimal? = null,
    /** Покупка у населения за смену по данным отчёта; пусто — такой операции в нём нет. */
    val buyTotal: Decimal? = null,
    val buyReturnTotal: Decimal? = null,
    /** Наличные в ящике на момент отчёта; не оплаченное наличными за смену. */
    val cashBalance: Decimal? = null,
    val receiptsCount: Int? = null,
    val kkmDocumentNumber: String? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    override val packet: String? = null
) : ProtocolDocument
