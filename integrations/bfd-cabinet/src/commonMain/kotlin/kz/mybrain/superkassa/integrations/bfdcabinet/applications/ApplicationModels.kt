package kz.mybrain.superkassa.integrations.bfdcabinet.applications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/** Заявление в ИСНА о кассе: постановка, перерегистрация или снятие с учёта. */
sealed interface CabinetApplication {
    /** Касса, о которой заявление. */
    val registerId: String

    /** Постановка на учёт. */
    data class Registration(override val registerId: String) : CabinetApplication

    /** Перерегистрация: смена торговой точки. */
    data class Reregistration(override val registerId: String, val request: ReregistrationRequest) :
        CabinetApplication

    /** Снятие с учёта. */
    data class Deregistration(override val registerId: String, val request: DeregistrationRequest) :
        CabinetApplication
}

/**
 * Подготовленное заявление: что подписать и до какого времени.
 *
 * @property payloadToSign то, что подписывается, в base64.
 */
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

/**
 * Подпись заявления.
 *
 * @property signatureCms CMS-подпись [ApplicationPrepared.payloadToSign] в base64.
 */
@Serializable
data class SignRequest(val actionId: String, val signatureCms: String)

/** Новая торговая точка прямо в заявлении о перерегистрации. */
@Serializable
data class NewRetailPlace(
    val name: String,
    val addressRef: String,
    val latitude: CabinetDecimal? = null,
    val longitude: CabinetDecimal? = null
)

/** Заявление о перерегистрации: на существующую точку или на новую. */
@Serializable
data class ReregistrationRequest(
    val newRetailPlaceId: String? = null,
    val newRetailPlace: NewRetailPlace? = null,
    val reason: String? = null
)

/** Заявление о снятии с учёта. */
@Serializable
data class DeregistrationRequest(val reason: String, val comment: String? = null)

/**
 * Регистрационное действие в журнале кассы.
 *
 * @property id идентификатор действия (`actionId`).
 * @property reasonCode код отказа ИСНА; `null` — не отказано.
 * @property processedAt когда действие завершено (`completedAt`).
 */
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
