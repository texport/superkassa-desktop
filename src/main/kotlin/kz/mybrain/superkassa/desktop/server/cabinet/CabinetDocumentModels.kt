package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** Техническое состояние кассы глазами сервера приёма. */
/**
 * Что о кассе знает сервис приёма. Кабинет отвечает признаком `active`
 * и причиной простоя; экранам состояние отдаётся тем же кодом, что и статус
 * кассы у сервера, — чтобы плашка была одна на все экраны.
 */
@Serializable
data class TechnicalState(
    val found: Boolean = false,
    val active: Boolean? = null,
    val inactiveReason: String? = null,
    val trafficSuspended: Boolean? = null,
    val ofdDisconnected: Boolean? = null,
    val billingStatus: Int? = null,
    val shiftStatus: String? = null,
    val shiftNumber: Int? = null,
    val validationMask: Int? = null,
    val lastContactAt: String? = null,
    val snapshotAt: String? = null
) {
    val status: String?
        get() = when {
            !found -> null
            active == true -> "KKM_ACTIVE"
            active == false -> "KKM_INACTIVE"
            else -> null
        }
}

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
    @SerialName("actionId") val id: String,
    val actionType: String,
    val status: String,
    val externalRequestId: String? = null,
    val registrationNumber: String? = null,
    val reasonCode: String? = null,
    val reasonMessage: String? = null,
    val createdAt: String? = null,
    val sentAt: String? = null,
    @SerialName("completedAt") val processedAt: String? = null,
    val stateSyncStatus: String? = null
)

/** Регистрационная карта кассы. */
@Serializable
data class RegistrationCard(
    val cashRegisterId: String,
    val status: String? = null,
    val companyBin: String? = null,
    val companyName: String? = null,
    val retailPlaceName: String? = null,
    val address: String? = null,
    val rka: String? = null,
    val cato: String? = null,
    val modelName: String? = null,
    val factoryNumber: String? = null,
    val kkmId: Long? = null,
    val registrationNumber: String? = null,
    val lastSuccessfulAction: String? = null,
    val updatedAt: String? = null,
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
