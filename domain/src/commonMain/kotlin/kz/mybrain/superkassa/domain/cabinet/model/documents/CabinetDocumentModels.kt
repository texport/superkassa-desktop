package kz.mybrain.superkassa.domain.cabinet.model.documents

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/** Техническое состояние кассы глазами сервера приёма. */
/**
 * Что о кассе знает сервис приёма. Кабинет отвечает признаком `active`
 * и причиной простоя; экранам состояние отдаётся тем же кодом, что и статус
 * кассы у сервера, — чтобы плашка была одна на все экраны.
 */
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
data class RegisterState(
    val cashRegisterId: String,
    val businessStatus: String,
    val stateSyncStatus: String? = null,
    val technicalState: TechnicalState? = null
)

/** Подготовленное заявление: что подписать и до какого времени. */
data class ApplicationPrepared(
    val actionId: String,
    val actionType: String,
    val payloadToSign: String,
    val expiresAt: String? = null
)

/** Подписанное заявление, ушедшее в ИСНА. */
data class ApplicationSent(
    val actionId: String,
    val actionStatus: String,
    val cashRegisterStatus: String? = null,
    val externalRequestId: String? = null
)

/** Подпись заявления. */
data class SignRequest(val actionId: String, val signatureCms: String)

/** Новая торговая точка прямо в заявлении о перерегистрации. */
data class NewRetailPlace(
    val name: String,
    val addressRef: String,
    val latitude: Decimal? = null,
    val longitude: Decimal? = null
)

/** Заявление о перерегистрации: смена точки. */
data class ReregistrationRequest(
    val newRetailPlaceId: String? = null,
    val newRetailPlace: NewRetailPlace? = null,
    val reason: String? = null
)

/** Заявление о снятии с учёта. */
data class DeregistrationRequest(val reason: String, val comment: String? = null)

/** Регистрационное действие в журнале кассы. */
data class RegistrationAction(
    val id: String,
    val actionType: String,
    val status: String,
    val externalRequestId: String? = null,
    val registrationNumber: String? = null,
    val reasonCode: String? = null,
    val reasonMessage: String? = null,
    val createdAt: String? = null,
    val sentAt: String? = null,
    val processedAt: String? = null,
    val stateSyncStatus: String? = null
)

/** Регистрационная карта кассы. */
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
data class RegistrationCardSummary(
    val available: Boolean = false,
    val status: String? = null,
    val pdfStatus: String? = null,
    val updatedAt: String? = null
)

/** Сколько чего накопила касса. */
data class DocumentsOverview(
    val cashRegisterId: String,
    val registrationCard: RegistrationCardSummary? = null,
    val receiptsCount: Long = 0,
    val reportsCount: Long = 0,
    val shiftsCount: Long = 0,
    val cashMovementsCount: Long = 0
)

/** Страница списка кабинета. */
data class CabinetPage<T>(
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val items: List<T> = emptyList()
)

/**
 * Длина страницы списка в кабинете.
 *
 * Отдельным предметом, а не полем одного из списков: размер страницы один
 * и тот же у чеков, смен, отчётов, движений денег, точек, касс и справочников.
 * Лежащий рядом с чеками, он читался бы как свойство чеков — и второй список
 * завёл бы своё число.
 */
internal const val PAGE_SIZE: Int = 50
