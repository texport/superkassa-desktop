package kz.mybrain.superkassa.desktop.eds

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Подпись ЭЦП через NCALayer.
 *
 * Ключ и пароль к нему остаются у владельца: приложение отправляет
 * в NCALayer то, что нужно подписать, а окно выбора сертификата и ввода
 * пароля показывает сам NCALayer. Ни ключ, ни пароль сюда не приходят
 * и нигде не хранятся.
 *
 * NCALayer слушает петлю по защищённому соединению с самоподписанным
 * сертификатом на `127.0.0.1`. Проверять его нечем и незачем: собеседник
 * — процесс на этой же машине, а не узел в сети. Доверие ограничено
 * этим соединением и не распространяется на обмен с кабинетом и узлом.
 */
class NcaLayer(private val address: String = DEFAULT_ADDRESS) {

    /**
     * Подписывает содержимое и возвращает CMS в base64.
     *
     * @param base64Content то, что подписывается, в base64 — как его выдал кабинет.
     * @return CMS-подпись в base64.
     * @throws EdsRefusal если NCALayer недоступен или владелец отказался подписывать.
     */
    suspend fun signCms(base64Content: String): String {
        val modern = runCatching { ncaSignatureOf(ask(ncaSignRequest(base64Content))) }
        modern.getOrNull()?.let { return it }
        val refusal = modern.exceptionOrNull() as? EdsRefusal ?: throw modern.exceptionOrNull()!!
        // Владелец закрыл окно — это его решение, повторять нечего.
        // Всё остальное может означать, что NCALayer этой версии
        // не знает модуль `basics`: у него есть прежний, и он работает
        // во всех выпусках, что стоят у владельцев.
        if (refusal.cancelled || refusal.problem != EdsProblem.Declined) throw refusal
        return ncaLegacySignatureOf(ask(ncaLegacyRequest(base64Content)))
    }

    /** Отвечает ли NCALayer на этой машине. */
    suspend fun available(): Boolean = runCatching {
        withTimeout(PROBE_TIMEOUT_MS) { client().use { it.webSocket(address) { } } }
    }.isSuccess

    /**
     * Запрос и ответ на него.
     *
     * NCALayer здоровается первым: сразу после подключения он присылает
     * кадр со своей версией. Принимать его за ответ нельзя — подпись
     * приходит следующим кадром, и приложение выбрасывало её, считая,
     * что владелец отказался подписывать, а затем открывало окно подписи
     * во второй раз. Поэтому кадры читаются, пока не придёт ответ по делу.
     *
     * Ожидание длинное: NCALayer держит соединение, пока владелец выбирает
     * сертификат и вводит пароль, и пять секунд оборвали бы подпись
     * на середине.
     */
    private suspend fun ask(request: JsonObject): JsonObject {
        val answer = try {
            client().use { http ->
                var received: JsonObject? = null
                withTimeout(SIGN_TIMEOUT_MS) {
                    http.webSocket(address) {
                        send(Frame.Text(request.toString()))
                        while (received == null) {
                            val text = (incoming.receive() as? Frame.Text)?.readText() ?: continue
                            received = parsed(text)?.takeUnless { it.isGreeting() }
                        }
                    }
                }
                received
            }
        } catch (failure: ClosedReceiveChannelException) {
            // Окно подписи закрыли, не ответив: NCALayer рвёт соединение
            // молча. Прежде это доходило до экрана именем класса — владелец
            // видел «Кабинет не отвечает» там, где сам и закрыл окно.
            throw EdsRefusal(EdsProblem.Declined, WINDOW_CLOSED, failure)
        } catch (failure: IOException) {
            throw EdsRefusal(EdsProblem.Unreachable, failure.message.orEmpty(), failure)
        } catch (failure: TimeoutCancellationException) {
            throw EdsRefusal(EdsProblem.Unreachable, failure.message.orEmpty(), failure)
        }
        return answer ?: throw EdsRefusal(EdsProblem.Unreachable, "")
    }

    /** Разбирает кадр; неразборный кадр ответом не считается. */
    private fun parsed(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()

    private fun client(): HttpClient = HttpClient(CIO) {
        install(WebSockets)
        engine {
            https {
                trustManager = LocalhostTrust
                random = SecureRandom()
            }
        }
    }

    companion object {
        /** Где NCALayer слушает на этой машине. */
        const val DEFAULT_ADDRESS: String = "wss://127.0.0.1:13579"

        /** Окно подписи закрыто владельцем — так это называется на экране. */
        private const val WINDOW_CLOSED = "\u041e\u043a\u043d\u043e \u043f\u043e\u0434\u043f\u0438\u0441\u0438 \u0437\u0430\u043a\u0440\u044b\u0442\u043e"

        private const val SIGN_TIMEOUT_MS = 180_000L
        private const val PROBE_TIMEOUT_MS = 2_000L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

/**
 * Запрос на подпись модулю `basics`.
 *
 * `decode` разворачивает переданный base64 перед подписью, `encapsulate`
 * кладёт подписанное внутрь CMS: кабинет принимает и присоединённую,
 * и отсоединённую подпись, но с присоединённой ему нечего сверять
 * с содержимым по памяти.
 */
internal fun ncaSignRequest(base64Content: String): JsonObject = buildJsonObject {
    put("module", "kz.gov.pki.knca.basics")
    put("method", "sign")
    putJsonObject("args") {
        put("format", "cms")
        put("data", base64Content)
        putJsonObject("signingParams") {
            put("decode", "true")
            put("encapsulate", "true")
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
        .joinToString(" · ")
    val detail = named.ifBlank { answer.toString() }
    return EdsRefusal(EdsProblem.Declined, detail.take(MAX_MESSAGE))
}

/** Прежний модуль отвечает кодом успеха строкой. */
private const val LEGACY_OK = "200"

/** Подпись документа: расширенное назначение ключа НУЦ РК. */
private const val SIGNING_OID = "1.3.6.1.5.5.7.3.4"

/** Сколько знаков сообщения NCALayer доходит до экрана. */
private const val MAX_MESSAGE = 200

/**
 * Доверие соединению с петлёй.
 *
 * Сертификат NCALayer самоподписан и меняется при переустановке;
 * проверять его цепочку не у кого. Собеседник опознан адресом
 * `127.0.0.1`, дальше этого соединения доверие не идёт.
 */
private object LocalhostTrust : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

/** Почему подпись не получена. */
enum class EdsProblem { Unreachable, Declined }

/** Отказ подписи, доведённый до экрана словами владельца. */
class EdsRefusal(
    val problem: EdsProblem,
    val detail: String,
    cause: Throwable? = null
) : Exception(detail, cause) {

    /**
     * Владелец сам закрыл окно подписи.
     *
     * Повторять запрос другим модулем в этом случае значит открыть окно
     * второй раз подряд там, где человек только что отказался.
     */
    val cancelled: Boolean
        get() = CANCEL_WORDS.any { detail.contains(it, ignoreCase = true) }

    private companion object {
        val CANCEL_WORDS = listOf("cancel", "отмен", "abort")
    }
}

/** То же для проверок: приветствие видно снаружи по одному полю версии. */
internal fun ncaIsGreeting(answer: JsonObject): Boolean = answer.isGreeting()

/** Обрамление PEM: строки `-----BEGIN …-----` и `-----END …-----`. */
private const val PEM_BOUNDARY = "-----"
