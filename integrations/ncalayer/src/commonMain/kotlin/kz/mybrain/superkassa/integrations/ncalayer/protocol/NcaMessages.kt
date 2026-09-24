package kz.mybrain.superkassa.integrations.ncalayer.protocol

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kz.mybrain.superkassa.integrations.ncalayer.NcaReason
import kz.mybrain.superkassa.integrations.ncalayer.NcaRefusal

/**
 * Запрос на подпись модулю `kz.gov.pki.knca.basics`.
 *
 * `decode` разворачивает переданный base64 перед подписью; `encapsulate`
 * выключен — служба проверяет подпись, подставляя данные сама, и вложенное
 * содержимое ей не нужно. `origin` NCALayer читает из заголовка рукопожатия,
 * а в запросе он оставлен для выпусков, которые смотрят сюда.
 */
internal fun ncaSignRequest(base64Content: String, origin: String, locale: String): JsonObject = buildJsonObject {
    put("module", BASICS)
    put("method", "sign")
    put("origin", origin)
    putJsonObject("args") {
        put("format", "cms")
        put("data", base64Content)
        putJsonObject("signingParams") {
            put("decode", "true")
            put("encapsulate", "false")
            put("digested", "false")
        }
        putJsonObject("signerParams") {
            put("extKeyUsageOids", buildJsonArray { add(JsonPrimitive(SIGNING_OID)) })
        }
        put("locale", locale)
    }
}

/**
 * Подпись из ответа модуля `basics`.
 *
 * Отказ владельца и недоступность ключа приходят одним полем `status: false`;
 * различают их слова NCALayer в [NcaRefusal.detail].
 */
internal fun ncaSignatureOf(answer: JsonObject): String {
    val result = (answer["body"] as? JsonObject)?.get("result")?.takeIf { answer.text("status") == "true" }
    return signatureIn(result) ?: throw declined(answer)
}

/**
 * Запрос на подпись прежнему модулю `commonUtils`.
 *
 * Модуль объявлен устаревшим, но стоит во всех выпусках NCALayer, какие
 * встречаются у владельцев. `PKCS12` — хранилище ключа, `SIGNATURE` —
 * назначение сертификата, последний признак — присоединённая подпись.
 */
internal fun ncaLegacyRequest(base64Content: String): JsonObject = buildJsonObject {
    put("module", LEGACY)
    put("method", "createCMSSignatureFromBase64")
    put(
        "args",
        buildJsonArray {
            add(JsonPrimitive("PKCS12"))
            add(JsonPrimitive("SIGNATURE"))
            add(JsonPrimitive(base64Content))
            add(JsonPrimitive(true))
        }
    )
}

/** Подпись из ответа прежнего модуля: код `200` и подпись строкой. */
internal fun ncaLegacySignatureOf(answer: JsonObject): String {
    val code = answer.text("code")
    if (code != null && code != LEGACY_OK) throw declined(answer)
    return signatureIn(answer["result"]) ?: throw declined(answer)
}

/**
 * Приветствие NCALayer: он присылает его первым кадром после подключения.
 *
 * Единственное поле `result.version` — ответом на подпись такой кадр быть
 * не может, а принятый за ответ, он выбрасывал пришедшую следом подпись.
 */
internal fun JsonObject.isGreeting(): Boolean {
    val result = this["result"] as? JsonObject ?: return false
    return size == 1 && result.keys == setOf("version")
}

/** Кому адресован запрос — модуль и метод: единственное о запросе, что идёт в журнал. */
internal fun ncaAddressee(request: JsonObject): String =
    listOf("module", "method").mapNotNull { request.text(it) }.joinToString("/")

/** Подпись из поля ответа: модуль `basics` кладёт её списком, прежний — строкой. */
private fun signatureIn(result: JsonElement?): String? {
    val value = when (result) {
        is JsonArray -> (result.firstOrNull() as? JsonPrimitive)?.content
        is JsonPrimitive -> result.content
        else -> null
    }
    return value?.let(::plainBase64)?.takeIf { it.isNotBlank() }
}

/**
 * Подпись одной строкой base64.
 *
 * NCALayer переносит строки и иногда обрамляет подпись `-----BEGIN CMS-----`;
 * строгий декодер службы такую строку отвергает. Байты подписи от чистки
 * не меняются.
 */
private fun plainBase64(value: String): String =
    value.lineSequence()
        .filterNot { it.startsWith(PEM_BOUNDARY) }
        .joinToString("")
        .filterNot { it.isWhitespace() }

/** Значение поля, если это строка или число. */
private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

/**
 * Отказ словами NCALayer; ответ незнакомой формы доходит целиком — без
 * подробностей не понять, чинить запрос или запускать NCALayer.
 *
 * Прежний модуль спросить стоит: незнакомый модуль NCALayer тоже отвергает
 * ответом, и отличить это от отказа владельца можно только по словам.
 */
private fun declined(answer: JsonObject): NcaRefusal {
    val named = listOfNotNull(answer.text("code"), answer.text("message")).filter { it.isNotBlank() }.joinToString(": ")
    val detail = named.ifBlank { answer.toString() }.take(MAX_DETAIL)
    return NcaRefusal(NcaReason.Declined, detail, askPreviousModule = true)
}

/** Модуль подписи текущих выпусков NCALayer. */
internal const val BASICS = "kz.gov.pki.knca.basics"

/** Прежний модуль подписи. */
internal const val LEGACY = "kz.gov.pki.knca.commonUtils"

/** Прежний модуль отвечает кодом успеха строкой. */
private const val LEGACY_OK = "200"

/** Подпись документа: расширенное назначение ключа НУЦ РК. */
private const val SIGNING_OID = "1.3.6.1.5.5.7.3.4"

/** Сколько знаков слов NCALayer сохраняется в отказе. */
private const val MAX_DETAIL = 200

/** Обрамление PEM. */
private const val PEM_BOUNDARY = "-----"
