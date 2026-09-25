package kz.mybrain.superkassa.integrations.egovmobile.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.integrations.egovmobile.EgovReason
import kz.mybrain.superkassa.integrations.egovmobile.EgovRefusal

/** Регистрация процедуры: описание eGov mobile показывает владельцу. */
@Serializable
internal data class Registration(val description: String)

/** Ответ на регистрацию: QR, ссылки запуска и адреса передачи данных и подписи. */
@Serializable
internal data class Registered(
    val expireAt: Long? = null,
    val qrCode: String? = null,
    val eGovMobileLaunchLink: String? = null,
    val dataURL: String? = null,
    val signURL: String? = null,
    val message: String? = null
)

/**
 * Данные на подпись: CMS без вложенных данных (`CMS_SIGN_ONLY`) — так же
 * подписывает NCALayer, и кабинет проверяет подпись, подставляя данные сам.
 */
@Serializable
internal data class ToSign(
    val signMethod: String = CMS_SIGN_ONLY,
    val version: Int = 1,
    val documentsToSign: List<DocumentToSign>
)

/** Один документ: названия на трёх языках и содержимое в base64. */
@Serializable
internal data class DocumentToSign(
    val id: Int,
    val nameRu: String,
    val nameKz: String,
    val nameEn: String,
    val meta: List<Meta> = emptyList(),
    val document: Document
)

/** Пара «имя — значение», которую eGov mobile показывает рядом с документом. */
@Serializable
internal data class Meta(val name: String, val value: String)

/** Обёртка содержимого документа. */
@Serializable
internal data class Document(val file: FileData)

/** Содержимое: вид (пусто — просто данные) и base64; в ответе здесь подпись. */
@Serializable
internal data class FileData(val mime: String = "", val data: String)

/** Ответ посредника на передачу данных или с подписями; `message` — отказ. */
@Serializable
internal data class Signed(
    val status: String? = null,
    val message: String? = null,
    val documentsToSign: List<DocumentToSign> = emptyList()
)

internal const val CMS_SIGN_ONLY = "CMS_SIGN_ONLY"

/** Отказ владельца в ответе посредника. */
internal const val CANCELED = "CANCELED"

/**
 * Разбор ответов: незнакомые поля посредника не ломают разбор. Значения
 * по умолчанию уходят посреднику: без них не ушёл бы и `signMethod`.
 */
internal val egovJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/** Сообщение посредника в JSON. */
internal inline fun <reified T> encoded(message: T): String = egovJson.encodeToString(message)

/** Ответ посредника из JSON; непонятный ответ — отказ, а не падение. */
internal inline fun <reified T> decoded(text: String): T = try {
    egovJson.decodeFromString<T>(text)
} catch (failure: SerializationException) {
    throw EgovRefusal(EgovReason.Refused, "answer not readable", failure)
}
