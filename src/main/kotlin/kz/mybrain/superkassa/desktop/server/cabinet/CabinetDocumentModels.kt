package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Состояние кассы, регистрационные действия и фискальные документы кабинета.
 *
 * Продолжение [CabinetModels]: там компания, точки и кассы, здесь — то,
 * что о кассе знает сервер приёма данных.
 */

/** Техническое состояние кассы глазами сервера приёма. */
@Serializable
data class TechnicalState(
    val found: Boolean = false,
    val status: String? = null,
    val trafficSuspended: Boolean? = null,
    val ofdDisconnected: Boolean? = null,
    val billingStatus: Int? = null,
    val shiftStatus: String? = null,
    val shiftNumber: Int? = null,
    val validationMask: Int? = null,
    val lastContactAt: String? = null,
    val snapshotAt: String? = null
)

/** Состояние кассы: учётное, синхронизация и техническое. */
@Serializable
data class RegisterState(
    val cashRegisterId: String,
    val businessStatus: String,
    val stateSyncStatus: String? = null,
    val technicalState: TechnicalState? = null
)

/** Подготовленное заявление: что подписать и до какого времени. */
@Serializable
data class ApplicationPrepared(
    val actionId: String,
    val actionType: String,
    val payloadToSign: String,
    val expiresAt: String? = null
)

/** Подписанное заявление, ушедшее в ИСНА. */
@Serializable
data class ApplicationSent(
    val actionId: String,
    val actionStatus: String,
    val cashRegisterStatus: String? = null,
    val externalRequestId: String? = null
)

/** Подпись заявления. */
@Serializable
data class SignRequest(val actionId: String, val signatureCms: String)

/** Новая торговая точка прямо в заявлении о перерегистрации. */
@Serializable
data class NewRetailPlace(
    val name: String,
    val addressRef: String,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null
)

/** Заявление о перерегистрации: смена точки. */
@Serializable
data class ReregistrationRequest(
    val newRetailPlaceId: String? = null,
    val newRetailPlace: NewRetailPlace? = null,
    val reason: String? = null
)

/** Заявление о снятии с учёта. */
@Serializable
data class DeregistrationRequest(val reason: String, val comment: String? = null)

/** Регистрационное действие в журнале кассы. */
@Serializable
data class RegistrationAction(
    val id: String,
    val actionType: String,
    val status: String,
    val externalRequestId: String? = null,
    val result: String? = null,
    val registrationNumber: String? = null,
    val reasonCode: String? = null,
    val reasonMessage: String? = null,
    val createdAt: String? = null,
    val sentAt: String? = null,
    val processedAt: String? = null,
    val stateSyncStatus: String? = null
)

/** Регистрационная карта кассы. */
@Serializable
data class RegistrationCard(
    val cashRegisterId: String,
    val status: String,
    val pdfStatus: String? = null,
    val updatedAt: String? = null,
    val actionId: String? = null,
    val deregisteredAt: String? = null,
    val deregistrationReason: String? = null
)

/** Сводка регистрационной карты в обзоре документов. */
@Serializable
data class RegistrationCardSummary(
    val available: Boolean = false,
    val status: String? = null,
    val pdfStatus: String? = null,
    val updatedAt: String? = null
)

/** Сколько чего накопила касса. */
@Serializable
data class DocumentsOverview(
    val cashRegisterId: String,
    val registrationCard: RegistrationCardSummary? = null,
    val receiptsCount: Long = 0,
    val reportsCount: Long = 0,
    val shiftsCount: Long = 0,
    val cashMovementsCount: Long = 0
)

/** Страница списка кабинета. */
@Serializable
data class CabinetPage<T>(
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val items: List<T> = emptyList()
)

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

/** Итоги смены. */
@Serializable
data class ShiftTotals(
    @Contextual val revenue: BigDecimal? = null,
    @Contextual val cashSum: BigDecimal? = null,
    val salesCount: Int = 0,
    @Contextual val salesSum: BigDecimal? = null,
    val returnsCount: Int = 0,
    @Contextual val returnsSum: BigDecimal? = null
)

/** Смена в списке. */
@Serializable
data class CabinetShift(
    val shiftNumber: Int,
    val state: String? = null,
    val openedAt: String? = null,
    val closedAt: String? = null,
    val zReportTransactionId: String? = null,
    @Contextual val total: BigDecimal? = null,
    val totals: ShiftTotals? = null
)

/** Отчёт в списке. */
@Serializable
data class CabinetReport(
    val transactionId: String,
    val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null,
    @Contextual val total: BigDecimal? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null
)

/** Внесение или изъятие в списке. */
@Serializable
data class CabinetCashMovement(
    val transactionId: String,
    val type: String? = null,
    @Contextual val amount: BigDecimal? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null
)

/** Сколько строк кабинет отдаёт за раз. */
const val PAGE_SIZE: Int = 50

/** Кассир, оформивший документ. */
@Serializable
data class DocumentOperator(val code: Int? = null, val name: String? = null)

/** Налог позиции или чека. */
@Serializable
data class DocumentTax(
    val type: String? = null,
    val percent: Int? = null,
    @Contextual val sum: BigDecimal? = null,
    val inTotalSum: Boolean? = null
)

/** Позиция чека. */
@Serializable
data class DocumentItem(
    val type: String? = null,
    val name: String? = null,
    val sectionCode: String? = null,
    @Contextual val quantity: BigDecimal? = null,
    @Contextual val price: BigDecimal? = null,
    @Contextual val sum: BigDecimal? = null,
    val measureUnitCode: String? = null,
    val barcode: String? = null,
    val taxes: List<DocumentTax> = emptyList()
)

/** Оплата чека. */
@Serializable
data class DocumentPayment(val type: String? = null, @Contextual val sum: BigDecimal? = null)

/** Итоги чека. */
@Serializable
data class DocumentAmounts(
    @Contextual val total: BigDecimal? = null,
    @Contextual val taken: BigDecimal? = null,
    @Contextual val change: BigDecimal? = null,
    @Contextual val discount: BigDecimal? = null,
    @Contextual val markup: BigDecimal? = null
)

/**
 * Чек целиком, как его принял ОФД.
 *
 * Поля, которых не было в сообщении кассы, равны `null`: ноль означает
 * присланный ноль, а не отсутствие — на этом различии держится разбор
 * расхождений с кассой.
 */
@Serializable
data class CabinetReceiptDetails(
    val transactionId: String,
    val receiptNumber: String? = null,
    val fiscalNumber: String? = null,
    val qrUrl: String? = null,
    val shiftNumber: Int? = null,
    val operationType: String? = null,
    @Contextual val total: BigDecimal? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null,
    val registrationNumber: String? = null,
    val operator: DocumentOperator? = null,
    val items: List<DocumentItem> = emptyList(),
    val payments: List<DocumentPayment> = emptyList(),
    val taxes: List<DocumentTax> = emptyList(),
    val amounts: DocumentAmounts? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null,
    val kgdMark: String? = null,
    val kgdMarkAt: String? = null
)

/** Отчёт целиком: смена, итоги и состояние доставки. */
@Serializable
data class CabinetReportDetails(
    val transactionId: String,
    val type: String? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null,
    val shiftOpenedAt: String? = null,
    val shiftClosedAt: String? = null,
    @Contextual val total: BigDecimal? = null,
    val registrationNumber: String? = null,
    val sendStatus: String? = null,
    val deliveryStatus: String? = null
)

/** Внесение или изъятие целиком: сумма, смена и кто оформил. */
@Serializable
data class CabinetCashMovementDetails(
    val transactionId: String,
    val type: String? = null,
    @Contextual val amount: BigDecimal? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null,
    val registrationNumber: String? = null,
    val operator: DocumentOperator? = null,
    val offline: Boolean? = null
)
