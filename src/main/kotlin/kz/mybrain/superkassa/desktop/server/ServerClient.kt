package kz.mybrain.superkassa.desktop.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
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
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.app.log.logged
import java.math.BigDecimal

/**
 * Отказ узла, доведённый до приложения целиком.
 *
 * Узел отвечает кодом и трёхъязычным текстом; приложение показывает кассиру
 * часть на его языке, а код сохраняет — по нему поддержка отличает
 * «недостаточно наличных» от «касса заблокирована», не читая журналы.
 */
class ServerRefusal(
    val code: String,
    val words: TrilingualText,
    val httpStatus: Int
) : Exception("$code: ${words.ru}")

/**
 * Обмен с узлом Суперкассы.
 *
 * Узел работает в режиме DESKTOP на этой же машине, поэтому обращение идёт
 * по петле и без шифрования: наружу порт не выставляется.
 */
class ServerClient(
    /**
     * Откуда брать адрес узла — при каждом обращении, а не однажды.
     *
     * Адрес меняют в настройках рабочего места, и перезапускать кассу ради
     * этого незачем: следующее обращение уходит уже по новому адресу.
     */
    private val address: () -> String = { DEFAULT_URL },
    private val http: HttpClient = defaultHttpClient()
) {
    /** Адрес узла с прежним именем: так его читают тесты и обращения ниже. */
    private val baseUrl: String get() = address().trimEnd('/')

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
        logged(LogSource.Node, method, path, body) {
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
        }

    /**
     * Обращение за ответом не в JSON — например, за картинкой печатной формы.
     *
     * Тело здесь тоже бывает: печатная форма документа, пробитого на другой
     * кассе, рисуется по переданным данным, а не по хранимому документу.
     */
    suspend fun callAccepting(
        method: HttpMethod,
        path: String,
        accept: ContentType,
        pin: String?,
        body: Any? = null
    ): HttpResponse =
        logged(LogSource.Node, method, path, body) {
            http.request(baseUrl + path) {
                this.method = method
                headers.append(HttpHeaders.Accept, accept.toString())
                if (pin != null) {
                    headers.append("Authorization", pin)
                }
                if (body != null) {
                    setBody(body)
                }
            }
        }

    suspend fun refusalOf(response: HttpResponse): ServerRefusal {
        val text = response.bodyAsText()
        val error = runCatching { lenientJson.decodeFromString<ServerError>(text) }.getOrNull()
        return ServerRefusal(
            code = error?.code ?: "HTTP_${response.status.value}",
            words = TrilingualText.of(error?.message ?: text.take(MAX_ERROR_LENGTH)),
            httpStatus = response.status.value
        )
    }

    fun close() = http.close()

    companion object {
        const val DEFAULT_URL: String = "http://127.0.0.1:8080"
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

        /**
         * Сколько касса ждёт соединения с узлом.
         *
         * Узел свой, на этой же машине: не ответил за секунды — значит
         * не поднят, и ждать его дольше нечего.
         */
        private const val CONNECT_WAIT_MS = 5_000L

        /**
         * Сколько касса ждёт ответа на обращение.
         *
         * Больше, чем узел сам ждёт ответа БФД (тридцать секунд), плюс
         * его собственная работа: запись документа, счётчики, печатная
         * форма. По умолчанию Ktor обрывал ожидание раньше узла, и чек,
         * который узел довёл до конца и БФД принял, касса объявляла
         * неудачей.
         */
        private const val ANSWER_WAIT_MS = 90_000L

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(ContentNegotiation) { json(lenientJson) }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_WAIT_MS
                requestTimeoutMillis = ANSWER_WAIT_MS
                socketTimeoutMillis = ANSWER_WAIT_MS
            }
        }
    }
}
