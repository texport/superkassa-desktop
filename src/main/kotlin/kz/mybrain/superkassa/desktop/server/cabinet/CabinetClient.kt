package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.app.log.logged
import kz.mybrain.superkassa.desktop.server.DecimalAsNumber
import java.math.BigDecimal

/**
 * Отказ кабинета, доведённый до приложения целиком.
 *
 * Кабинет отвечает кодом и сообщением на одном языке — в отличие от узла,
 * который присылает три. Код сохраняется: по нему видно, истекла ли
 * сессия (`UNAUTHORIZED`) или заявление отвергнуто ИСНА.
 */
class CabinetRefusal(
    val code: String,
    val text: String,
    val httpStatus: Int
) : Exception("$code: $text")

/**
 * Обмен с личным кабинетом ОФД.
 *
 * Кабинет — отдельная служба со своим адресом: она может стоять и на этой
 * машине, и на стенде, поэтому адрес задаётся в настройках рабочего места,
 * а не зашит. Доступ выдаётся по ЭЦП и живёт до `expiresAt`; токен держит
 * [kz.mybrain.superkassa.desktop.app.CabinetSession], а не клиент —
 * клиент не знает, кто вошёл.
 */
class CabinetClient(
    var baseUrl: String = DEFAULT_URL,
    private val http: HttpClient = defaultHttpClient()
) {
    /**
     * От чьего имени идут запросы в режиме разработки кабинета.
     *
     * На период MVP кабинет не требует входа по ЭЦП: пользователя и компанию
     * он берёт из заголовков `X-Debug-Iin` и `X-Debug-Bin`, а без них —
     * из своей настройки. Пока личность задана, доступ по ЭЦП не передаётся:
     * его у такого сеанса нет.
     */
    var debugIdentity: DebugIdentity? = null

    /** Выполняет запрос и разбирает ответ, отделяя отказ от недоступности. */
    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        token: String? = null,
        idempotencyKey: String? = null
    ): T {
        val response = call(method, path, body, token, idempotencyKey)
        if (!response.status.isSuccess()) {
            throw refusalOf(response)
        }
        return response.body()
    }

    suspend fun call(
        method: HttpMethod,
        path: String,
        body: Any?,
        token: String?,
        idempotencyKey: String? = null
    ): HttpResponse =
        logged(LogSource.Cabinet, method, path, body) {
            http.request(baseUrl + path) {
                this.method = method
                contentType(ContentType.Application.Json)
                identify(token)
                if (idempotencyKey != null) {
                    headers.append("Idempotency-Key", idempotencyKey)
                }
                if (body != null) {
                    setBody(body)
                }
            }
        }

    /** Ответ не в JSON — например, PDF регистрационной карты. */
    suspend fun bytes(path: String, accept: ContentType, token: String?): ByteArray {
        val response = logged(LogSource.Cabinet, HttpMethod.Get, path, body = null) {
            http.request(baseUrl + path) {
                method = HttpMethod.Get
                headers.append(HttpHeaders.Accept, accept.toString())
                identify(token)
            }
        }
        if (!response.status.isSuccess()) {
            throw refusalOf(response)
        }
        return response.bodyAsBytes()
    }

    private fun HttpRequestBuilder.identify(token: String?) {
        val identity = debugIdentity
        if (identity != null) {
            headers.append(DebugIdentity.IIN_HEADER, identity.iin)
            headers.append(DebugIdentity.BIN_HEADER, identity.bin)
            return
        }
        if (token != null) {
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun refusalOf(response: HttpResponse): CabinetRefusal {
        val text = response.bodyAsText()
        val error = runCatching { lenientJson.decodeFromString<CabinetError>(text) }.getOrNull()
        return CabinetRefusal(
            code = error?.code ?: "HTTP_${response.status.value}",
            text = error?.text() ?: text.take(MAX_ERROR_LENGTH),
            httpStatus = response.status.value
        )
    }

    fun close() = http.close()

    companion object {
        /** Кабинет рядом с узлом: у узла занят 8080, поэтому по умолчанию 8090. */
        const val DEFAULT_URL: String = "http://bfd-cabinet.ecc.kz"

        private const val MAX_ERROR_LENGTH = 200

        val lenientJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            serializersModule = SerializersModule {
                contextual(BigDecimal::class, DecimalAsNumber)
            }
        }

        /**
         * Клиент кабинета со сроками ожидания.
         *
         * Сроков не было вовсе, и молчащий кабинет держал приложение
         * сколько угодно: владелец подписал заявление, запрос ушёл —
         * и экран остался занятым навсегда, без слова о причине.
         * Отдельные сроки на соединение и на ответ: недоступную службу
         * видно сразу, а долгий ответ на обычный запрос ждать незачем.
         */
        /** Сколько ждать соединения с кабинетом: его нет или он не слушает. */
        private const val CONNECT_WAIT_MS = 10_000L

        /**
         * Сколько ждать ответа на запрос.
         *
         * Кабинет отвечает за сотни миллисекунд, самые долгие ответы —
         * выпуск токена и подача заявления в КГД. Полминуты с запасом
         * покрывают их и не оставляют экран занятым без конца.
         */
        private const val ANSWER_WAIT_MS = 30_000L

        /** Сколько ждать перед второй попыткой соединения. */
        private const val RECONNECT_WAIT_MS = 400L

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(ContentNegotiation) { json(lenientJson) }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_WAIT_MS
                requestTimeoutMillis = ANSWER_WAIT_MS
                socketTimeoutMillis = ANSWER_WAIT_MS
            }
            // Вторая попытка — только когда соединение не поднялось вовсе.
            // Кабинет стоит за VPN, и первая попытка после простоя
            // упиралась в неподнявшийся туннель: владелец читал «Кабинет
            // не отвечает» и нажимал то же самое второй раз руками.
            //
            // Повтор безопасен именно в этом случае и только в нём: пока
            // соединения нет, запрос не ушёл, и повторить можно даже подачу
            // заявления. Истекшее ожидание ответа не повторяется — запрос
            // мог дойти, и второе заявление было бы вторым заявлением.
            install(HttpRequestRetry) {
                maxRetries = 1
                retryIf { _, _ -> false }
                retryOnExceptionIf { _, failure -> failure is ConnectTimeoutException }
                delayMillis { RECONNECT_WAIT_MS }
            }
        }
    }
}

/** Отказ кабинета как он приходит по сети. */
/**
 * Отказ кабинета. Кабинет отвечает по RFC 9457: причина в `detail`,
 * заголовок в `title`; прежняя форма с `message` тоже читается.
 */
@kotlinx.serialization.Serializable
data class CabinetError(
    val code: String? = null,
    val message: String? = null,
    val detail: String? = null,
    val title: String? = null,
    val errors: List<CabinetFieldError> = emptyList()
) {
    /**
     * Что сказать владельцу.
     *
     * Сказанное о полях идёт первым: на неверный ввод кабинет отвечает
     * общим `detail` — «Validation failure», — а по существу говорит
     * в `errors`, по-русски и про то поле, которое надо исправить.
     * Владелец читал английское слово, не сообщавшее ему ничего.
     */
    fun text(): String? {
        val fields = errors.mapNotNull { it.message }.joinToString("; ").takeIf { it.isNotBlank() }
        return listOfNotNull(fields, message, detail, title).firstOrNull { it.isNotBlank() }
    }
}

@kotlinx.serialization.Serializable
data class CabinetFieldError(val field: String? = null, val message: String? = null)

/** Личность владельца в режиме разработки кабинета: ИИН пользователя и БИН компании. */
data class DebugIdentity(val iin: String, val bin: String) {
    companion object {
        const val IIN_HEADER: String = "X-Debug-Iin"
        const val BIN_HEADER: String = "X-Debug-Bin"
    }
}
