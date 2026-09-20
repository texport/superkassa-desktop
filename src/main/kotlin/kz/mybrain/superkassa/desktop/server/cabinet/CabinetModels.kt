package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
/**
 * Ответы кабинета: владелец, компания и виды деятельности.
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

/** Позиция классификатора ОКЭД, какой её отдаёт кабинет. */
@Serializable
data class OkedEntry(
    val code: String,
    val name: String,
    val nameKz: String? = null,
    /** Уровень подробности: раздел, группа, класс, подкласс, вид. */
    val level: String? = null
)

/** Ответ классификатора: обёртка списка той же формы, что у адресных подсказок. */
@Serializable
data class OkedSuggestions(val items: List<OkedEntry> = emptyList())

/** Замена набора видов деятельности целиком. */
@Serializable
data class OkedsRequest(val okeds: List<Oked>)

/**
 * Ответ на замену видов деятельности.
 *
 * Кабинет отвечает карточкой компании; прежний отвечал одними видами
 * с `companyId`. Читаются оба: клиент, ждавший одну форму, падал
 * на разборе удавшегося сохранения.
 */
@Serializable
data class OkedsView(val id: String? = null, val companyId: String? = null, val okeds: List<Oked> = emptyList())
