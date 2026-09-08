package kz.mybrain.superkassa.desktop.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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
import java.math.BigDecimal

/**
 * Отказ узла, доведённый до приложения целиком.
 *
 * Узел отвечает кодом и трёхъязычным текстом; приложение показывает кассиру
 * русскую часть, но код сохраняет — по нему поддержка отличает «недостаточно
 * наличных» от «касса заблокирована», не читая журналы.
 */
class ServerRefusal(
    val code: String,
    val russianText: String,
    val httpStatus: Int
) : Exception("$code: $russianText")

/**
 * Обмен с узлом Суперкассы.
 *
 * Узел работает в режиме DESKTOP на этой же машине, поэтому обращение идёт
 * по петле и без шифрования: наружу порт не выставляется.
 */
class ServerClient(
    private val baseUrl: String = DEFAULT_URL,
    private val http: HttpClient = defaultHttpClient()
) {
    /**
     * Выполняет запрос и разбирает ответ, отделяя отказ по существу
     * от недоступности узла.
     */
    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        pin: String? = null
    ): T {
        val response = call(method, path, body, pin)
        if (!response.status.isSuccess()) {
            throw refusalOf(response)
        }
        return response.body()
    }

    suspend fun call(method: HttpMethod, path: String, body: Any?, pin: String?): HttpResponse =
        http.request(baseUrl + path) {
            this.method = method
            contentType(ContentType.Application.Json)
            if (pin != null) {
                headers.append("Authorization", pin)
            }
            if (body != null) {
                setBody(body)
            }
        }

    /**
     * Обращение за ответом не в JSON — например, за картинкой печатной формы.
     */
    suspend fun callAccepting(
        method: HttpMethod,
        path: String,
        accept: ContentType,
        pin: String?
    ): HttpResponse =
        http.request(baseUrl + path) {
            this.method = method
            headers.append(HttpHeaders.Accept, accept.toString())
            if (pin != null) {
                headers.append("Authorization", pin)
            }
        }

    suspend fun refusalOf(response: HttpResponse): ServerRefusal {
        val text = response.bodyAsText()
        val error = runCatching { lenientJson.decodeFromString<ServerError>(text) }.getOrNull()
        return ServerRefusal(
            code = error?.code ?: "HTTP_${response.status.value}",
            russianText = russianPart(error?.message) ?: text.take(MAX_ERROR_LENGTH),
            httpStatus = response.status.value
        )
    }

    /** Узел склеивает три языка в одну строку — кассиру нужна одна. */
    private fun russianPart(message: String?): String? {
        val text = message?.takeIf { it.isNotBlank() } ?: return null
        val start = text.indexOf(RU_PREFIX)
        if (start < 0) return text
        val rest = text.substring(start + RU_PREFIX.length)
        return rest.substringBefore(SEPARATOR).trim()
    }

    fun close() = http.close()

    companion object {
        const val DEFAULT_URL: String = "http://127.0.0.1:8080"
        private const val RU_PREFIX = "RU:"
        private const val SEPARATOR = " | "
        private const val MAX_ERROR_LENGTH = 200

        val lenientJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            // Суммы и количества едут точной десятичной записью,
            // а не через плавающую точку: см. [DecimalAsNumber].
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
