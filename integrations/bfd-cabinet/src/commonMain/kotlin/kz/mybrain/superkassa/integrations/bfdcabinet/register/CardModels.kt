@file:OptIn(ExperimentalSerializationApi::class)

package kz.mybrain.superkassa.integrations.bfdcabinet.register

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Регистрационная карта кассы, выданная КГД.
 *
 * @property kkmId номер кассы в БФД.
 * @property registrationNumber номер КГД.
 * @property lastSuccessfulAction последнее удавшееся регистрационное действие.
 */
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

/**
 * Версия регистрационной карты.
 *
 * При перерегистрации карта переписывается, а прежняя нужна — ею
 * подтверждают, где касса стояла в те дни. Имена полей у кабинета ещё
 * сдвинутся, поэтому у спорных перечислены возможные.
 *
 * @property version номер версии.
 * @property validFrom когда версия открыта.
 * @property validTo когда закрыта; у действующей — ничего.
 * @property openedBy каким действием открыта: постановка или перерегистрация.
 * @property closedBy чем закрыта: перерегистрацией или снятием с учёта.
 * @property changed что стало другим против предыдущей версии.
 * @property current действующая ли, по словам кабинета.
 */
@Serializable
data class RegistrationCardVersion(
    @JsonNames("versionNumber", "number") val version: Int = 0,
    val status: String? = null,
    @JsonNames("openedAt", "createdAt") val validFrom: String? = null,
    @JsonNames("closedAt") val validTo: String? = null,
    @JsonNames("openedByActionType", "openAction") val openedBy: String? = null,
    @JsonNames("closedByActionType", "closeAction") val closedBy: String? = null,
    @JsonNames("changedFields", "changes") val changed: List<String> = emptyList(),
    @JsonNames("active", "isCurrent") val current: Boolean = false
) {
    /** Действует ли версия сейчас: так названа кабинетом или ещё не закрыта. */
    val open: Boolean get() = current || validTo.isNullOrBlank()
}
