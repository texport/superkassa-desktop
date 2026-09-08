package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
    /** Выполняет запрос и разбирает ответ, отделяя отказ от недоступности. */
    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        token: String? = null
    ): T {
        val response = call(method, path, body, token)
        if (!response.status.isSuccess()) {
            throw refusalOf(response)
        }
        return response.body()
    }

    suspend fun call(method: HttpMethod, path: String, body: Any?, token: String?): HttpResponse =
        http.request(baseUrl + path) {
            this.method = method
            contentType(ContentType.Application.Json)
            if (token != null) {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
            if (body != null) {
                setBody(body)
            }
        }

    /** Ответ не в JSON — например, PDF регистрационной карты. */
    suspend fun bytes(path: String, accept: ContentType, token: String?): ByteArray {
        val response = http.request(baseUrl + path) {
            method = HttpMethod.Get
            headers.append(HttpHeaders.Accept, accept.toString())
            if (token != null) {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        if (!response.status.isSuccess()) {
            throw refusalOf(response)
        }
        return response.bodyAsBytes()
    }

    suspend fun refusalOf(response: HttpResponse): CabinetRefusal {
        val text = response.bodyAsText()
        val error = runCatching { lenientJson.decodeFromString<CabinetError>(text) }.getOrNull()
        return CabinetRefusal(
            code = error?.code ?: "HTTP_${response.status.value}",
            text = error?.message?.takeIf { it.isNotBlank() } ?: text.take(MAX_ERROR_LENGTH),
            httpStatus = response.status.value
        )
    }

    fun close() = http.close()

    companion object {
        /** Кабинет рядом с узлом: у узла занят 8080, поэтому по умолчанию 8090. */
        const val DEFAULT_URL: String = "http://127.0.0.1:8090"

        private const val MAX_ERROR_LENGTH = 200

        val lenientJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            serializersModule = SerializersModule {
                contextual(BigDecimal::class, DecimalAsNumber)
            }
        }

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(ContentNegotiation) { json(lenientJson) }
        }
    }
}

/** Отказ кабинета как он приходит по сети. */
@kotlinx.serialization.Serializable
data class CabinetError(val code: String? = null, val message: String? = null)
