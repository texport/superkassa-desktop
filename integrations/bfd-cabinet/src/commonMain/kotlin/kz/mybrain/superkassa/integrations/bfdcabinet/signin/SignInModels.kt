package kz.mybrain.superkassa.integrations.bfdcabinet.signin

import kotlinx.serialization.Serializable

/**
 * Владелец, вошедший по ЭЦП.
 *
 * @property id идентификатор пользователя в кабинете.
 * @property iin ИИН владельца.
 * @property fullName ФИО, как его знает кабинет.
 */
@Serializable
data class CabinetUser(val id: String, val iin: String, val fullName: String)

/**
 * Компания владельца.
 *
 * @property id идентификатор компании в кабинете.
 * @property bin БИН компании.
 * @property name наименование.
 */
@Serializable
data class CabinetCompany(val id: String, val bin: String, val name: String)

/** Кто вошёл в кабинет: владелец и его компания — всегда вместе. */
data class CabinetOwner(val user: CabinetUser, val company: CabinetCompany)

/**
 * Ответ на вход: доступ выдан до `expiresAt`.
 *
 * @property accessToken доступ владельца; наружу модуля не выходит.
 * @property expiresAt момент ISO-8601, до которого доступ действует.
 */
@Serializable
internal class CabinetLogin(
    val accessToken: String,
    val expiresAt: String? = null,
    val user: CabinetUser,
    val company: CabinetCompany
)

/**
 * Кто вошёл — по действующему доступу.
 *
 * @property expiresAt момент ISO-8601, до которого доступ действует.
 */
@Serializable
data class CabinetMe(val user: CabinetUser, val company: CabinetCompany, val expiresAt: String? = null)

/** Задача на подпись: что подписать и до какого времени. */
@Serializable
internal class EdsChallenge(val challengeId: String, val payload: String, val expiresAt: String? = null)

/** Вход по ЭЦП: подпись задачи. */
@Serializable
internal class EdsLoginRequest(val challengeId: String, val signatureCms: String)
