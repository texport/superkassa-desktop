package kz.mybrain.superkassa.desktop.eds

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Запрос на подпись модулю `basics`.
 *
 * `decode` разворачивает переданный base64 перед подписью, `encapsulate`
 * кладёт подписанное внутрь CMS: кабинет принимает и присоединённую,
 * и отсоединённую подпись, но с присоединённой ему нечего сверять
 * с содержимым по памяти.
 *
 * `origin` — то, чьё имя NCALayer покажет владельцу в окне подписи:
 * «{0} запрашивает разрешение». Без него окно называет просителя
 * `UNIDENTIFIED`, и владелец подписывает, не зная кому. Поле читает сам
 * модуль подписи — `BasicsModuleService.process` и `PKIExtras` берут его
 * из запроса значением по умолчанию `UNIDENTIFIED`.
 */
internal fun ncaSignRequest(base64Content: String): JsonObject = buildJsonObject {
    put("module", "kz.gov.pki.knca.basics")
    put("method", "sign")
    put("origin", ORIGIN)
    putJsonObject("args") {
        put("format", "cms")
        put("data", base64Content)
        putJsonObject("signingParams") {
            put("decode", "true")
            // Кабинет проверяет подпись, подставляя данные сам: CMS без вложенного
            // содержимого (`CMSSignedData(payload, cms)` у BouncyCastle).
            put("encapsulate", "false")
            put("digested", "false")
        }
        putJsonObject("signerParams") {
            put("extKeyUsageOids", buildJsonArray { add(JsonPrimitive(SIGNING_OID)) })
        }
        put("locale", "ru")
    }
}

/**
 * Подпись из ответа.
 *
 * Отказ владельца и недоступность ключа приходят одним и тем же полем
 * `status: false`, но владельцу это разные события: первое исправляется
 * повторным нажатием, второе — запуском NCALayer.
 */
internal fun ncaSignatureOf(answer: JsonObject): String {
    if (answer.text("status") != "true") throw declined(answer)
    val body = answer["body"] as? JsonObject ?: throw declined(answer)
    return signatureIn(body["result"]) ?: throw declined(answer)
}

/**
 * Запрос на подпись прежнему модулю `commonUtils`.
 *
 * Модуль объявлен устаревшим, но стоит во всех выпусках NCALayer, какие
 * встречаются у владельцев. `PKCS12` — хранилище ключа, `SIGNATURE` —
 * назначение сертификата, последний признак означает присоединённую
 * подпись.
 */
internal fun ncaLegacyRequest(base64Content: String): JsonObject = buildJsonObject {
    put("module", "kz.gov.pki.knca.commonUtils")
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
 * Подпись из поля ответа.
 *
 * Модуль `basics` кладёт её списком, прежний — строкой. Разбирать обе
 * формы здесь дешевле, чем держать два почти одинаковых чтения ответа.
 */
private fun signatureIn(result: JsonElement?): String? {
    val value = when (result) {
        is JsonArray -> (result.firstOrNull() as? JsonPrimitive)?.content
        is JsonPrimitive -> result.content
        else -> null
    }
    return value?.let { plainBase64(it) }?.takeIf { it.isNotBlank() }
}

/**
 * Подпись одной строкой base64.
 *
 * NCALayer переносит строки каждые несколько десятков знаков, а иногда
 * обрамляет подпись строками `-----BEGIN CMS-----`. Кабинет разбирает
 * строгим декодером и такую строку отвергает: `signatureCms is not valid
 * base64`. Байты подписи от чистки не меняются — убираются только
 * обрамление и переносы.
 */
private fun plainBase64(value: String): String =
    value.lineSequence()
        .filterNot { it.startsWith(PEM_BOUNDARY) }
        .joinToString("")
        .filterNot { it.isWhitespace() }

/**
 * Приветствие NCALayer: он присылает его первым кадром после подключения.
 *
 * Единственное поле `result.version` и ничего больше — ответом на подпись
 * такой кадр быть не может.
 */
internal fun JsonObject.isGreeting(): Boolean {
    val result = this["result"] as? JsonObject ?: return false
    return size == 1 && result.keys == setOf("version")
}

/** Значение поля, если это строка или число; иначе `null`. */
private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

/**
 * Отказ подписи, названный словами NCALayer.
 *
 * Когда код и сообщение на месте — берутся они. Когда ответ незнакомой
 * формы, до экрана доходит он сам: «подпись не получена» без единой
 * подробности не даёт понять, чинить запрос или запускать NCALayer.
 */
private fun declined(answer: JsonObject): EdsRefusal {
    val named = listOfNotNull(answer.text("code"), answer.text("message"))
        .filter { it.isNotBlank() }
        .joinToString(Glyphs.SEPARATOR)
    val detail = named.ifBlank { answer.toString() }
    return EdsRefusal(EdsProblem.Declined, detail.take(MAX_MESSAGE))
}

/** Прежний модуль отвечает кодом успеха строкой. */
private const val LEGACY_OK = "200"

/**
 * Чьё имя NCALayer показывает владельцу в окне подписи.
 *
 * Латиницей и без пояснений: строка встаёт в шаблон «{0} запрашивает
 * разрешение» и должна читаться названием приложения, а не предложением.
 */
internal const val ORIGIN = "Superkassa"

/** Подпись документа: расширенное назначение ключа НУЦ РК. */
private const val SIGNING_OID = "1.3.6.1.5.5.7.3.4"

/** Сколько знаков сообщения NCALayer доходит до экрана. */
private const val MAX_MESSAGE = 200

/** То же для проверок: приветствие видно снаружи по одному полю версии. */
internal fun ncaIsGreeting(answer: JsonObject): Boolean = answer.isGreeting()

/** Обрамление PEM: строки `-----BEGIN …-----` и `-----END …-----`. */
private const val PEM_BOUNDARY = "-----"
