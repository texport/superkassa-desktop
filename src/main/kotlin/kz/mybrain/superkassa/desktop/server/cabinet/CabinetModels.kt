package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Ответы личного кабинета ОФД.
 *
 * Имена полей повторяют контракт кабинета (`docs/openapi.yaml`) буква
 * в букву: своё название здесь означало бы молчаливое расхождение при
 * первой же правке на стороне кабинета.
 *
 * Время приходит строкой ISO-8601 и хранится строкой: в кабинете оно
 * нужно только для показа, а разбор ради показа стоил бы своего типа
 * и своего часового пояса.
 */

/** Владелец, вошедший по ЭЦП. */
@Serializable
data class CabinetUser(val id: String, val iin: String, val fullName: String)

/** Компания владельца. */
@Serializable
data class CabinetCompany(val id: String, val bin: String, val name: String)

/** Ответ на вход: доступ выдан до `expiresAt`. */
@Serializable
data class CabinetLogin(
    val accessToken: String,
    val expiresAt: String? = null,
    val user: CabinetUser,
    val company: CabinetCompany
)

/** Кто вошёл — по действующему доступу. */
@Serializable
data class CabinetMe(
    val user: CabinetUser,
    val company: CabinetCompany,
    val expiresAt: String? = null
)

/** Задача на подпись: что подписать и до какого времени. */
@Serializable
data class EdsChallenge(val challengeId: String, val payload: String, val expiresAt: String? = null)

/** Вход по ЭЦП: подпись задачи. */
@Serializable
data class EdsLoginRequest(val challengeId: String, val signatureCms: String)

/** Вид деятельности компании. */
@Serializable
data class Oked(
    val code: String,
    val name: String? = null,
    /**
     * Основной ли это вид деятельности.
     *
     * Значение пишется в запрос всегда, даже когда оно совпадает
     * со значением по умолчанию: kotlinx.serialization по умолчанию
     * умолчания опускает, а кабинет ждёт примитив `boolean` и на
     * отсутствующем поле отвечает отказом разбора. Ломалось это
     * не на первом виде деятельности, а на втором — первый становится
     * основным сам, и `true` в запрос попадал.
     */
    @EncodeDefault
    val primary: Boolean = false
)

/** Компания и её виды деятельности. */
@Serializable
data class CompanyProfile(
    val id: String,
    val bin: String,
    val name: String,
    val okeds: List<Oked> = emptyList()
)

/** Замена набора видов деятельности целиком. */
@Serializable
data class OkedsRequest(val okeds: List<Oked>)

/**
 * Ответ на замену видов деятельности.
 *
 * Кабинет отвечает не карточкой компании, а только её видами: клиент,
 * ждавший карточку, падал на разборе удавшегося сохранения.
 */
@Serializable
data class OkedsView(val companyId: String, val okeds: List<Oked> = emptyList())

/** Торговая точка компании. */
@Serializable
data class RetailPlace(
    val id: String,
    val name: String,
    val addressRef: String? = null,
    val rka: String? = null,
    val cato: String? = null,
    val address: String? = null,
    val addressKz: String? = null,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null,
    val cashRegisterCount: Long = 0
)

/** Заведение торговой точки. */
@Serializable
data class RetailPlaceCreate(
    val name: String,
    val addressRef: String,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null
)

/** Переименование торговой точки. */
@Serializable
data class RetailPlaceRename(val name: String)

/** Смена адреса торговой точки. */
@Serializable
data class RetailPlaceAddress(
    val addressRef: String,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null
)

/** Адрес из регистра. */
@Serializable
data class RegisterAddress(
    val addressRef: String,
    val address: String? = null,
    val addressKz: String? = null,
    val rka: String? = null,
    val cato: String? = null
)

/** Список адресов регистра. */
@Serializable
data class RegisterAddresses(val items: List<RegisterAddress> = emptyList())

/**
 * Вид деятельности из классификатора ОКЭД.
 *
 * Отличается от [Oked] компании: там — что владелец за собой записал
 * и какой из видов основной, здесь — что вообще существует в классификаторе.
 */
@Serializable
data class OkedEntry(val code: String, val name: String = "", val nameKz: String = "")

/** Модель кассового аппарата из справочника. */
@Serializable
data class KkmModel(val modelCode: String, val name: String? = null, val active: Boolean = true)

/** Модель в карточке кассы. */
@Serializable
data class CashRegisterModel(val modelCode: String, val name: String? = null)

/** Торговая точка в карточке кассы. */
@Serializable
data class RetailPlaceRef(val id: String, val name: String? = null)

/** Последнее регистрационное действие в карточке. */
@Serializable
data class LastRegistrationAction(val type: String, val status: String, val at: String? = null)

/** Касса в списке кабинета. */
@Serializable
data class CabinetRegister(
    val id: String,
    val kkmId: Int,
    val internalName: String? = null,
    val status: String,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int = 0,
    val model: CashRegisterModel? = null,
    val retailPlace: RetailPlaceRef? = null,
    val lastRegistrationAction: LastRegistrationAction? = null,
    val registrationCardAvailable: Boolean = false
)

/** Заведение кассы. */
@Serializable
data class RegisterCreate(
    val retailPlaceId: String,
    val modelCode: String,
    val factoryNumber: String,
    val manufactureYear: Int,
    val internalName: String? = null
)

/** Правка заведённой кассы. */
@Serializable
data class RegisterEdit(
    val retailPlaceId: String? = null,
    val modelCode: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int? = null
)

/** Своё название кассы. */
@Serializable
data class InternalNameRequest(val internalName: String?)

/** Выданный кассе технический токен. */
@Serializable
data class TokenIssued(val kkmId: Int, val token: Long)
