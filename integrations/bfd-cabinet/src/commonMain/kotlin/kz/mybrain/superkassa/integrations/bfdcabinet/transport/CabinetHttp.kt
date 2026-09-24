package kz.mybrain.superkassa.integrations.bfdcabinet.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetJournal
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetRefusal
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetUnreachable
import kz.mybrain.superkassa.integrations.bfdcabinet.DevelopmentIdentity
import kotlin.time.TimeSource

/**
 * Сеть кабинета: сроки, повтор соединения, личность и журнал.
 *
 * Отказ отделяется от недоступности здесь, один раз на все ручки: ответ
 * не успехом — [CabinetRefusal], ответа нет — [CabinetUnreachable].
 */
internal class CabinetHttp(
    private val settings: CabinetSettings,
    private val journal: CabinetJournal,
    engine: HttpClientEngine
) {
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = settings.connectWait.inWholeMilliseconds
            requestTimeoutMillis = settings.answerWait.inWholeMilliseconds
            socketTimeoutMillis = settings.answerWait.inWholeMilliseconds
        }
        // Вторая попытка — только когда соединение не поднялось вовсе: кабинет
        // стоит за VPN, и первая попытка после простоя упиралась в туннель.
        // Пока соединения нет, запрос не ушёл, и повторить можно даже подачу
        // заявления; истекшее ожидание ответа не повторяется — запрос мог дойти.
        install(HttpRequestRetry) {
            maxRetries = 1
            retryIf { _, _ -> false }
            retryOnExceptionIf { _, failure -> failure is ConnectTimeoutException }
            delayMillis { settings.reconnectPause.inWholeMilliseconds }
        }
    }

    /** Адрес кабинета. */
    val address: String get() = settings.baseUrl

    /**
     * Обращение и ответ успехом.
     *
     * @throws CabinetRefusal кабинет ответил отказом.
     * @throws CabinetUnreachable кабинет не ответил.
     */
    suspend fun send(call: CabinetCall): HttpResponse {
        val started = TimeSource.Monotonic.markNow()
        val line = "${call.method.value} ${call.path.substringBefore('?')}"
        val response = try {
            client.request(settings.baseUrl.trimEnd('/') + call.path) { prepare(call) }
        } catch (failure: IOException) {
            journal.record("$line failed", failure)
            throw CabinetUnreachable(failure)
        }
        journal.record("$line -> ${response.status.value} in ${started.elapsedNow().inWholeMilliseconds} ms", null)
        if (!response.status.isSuccess()) throw refusalOf(response)
        return response
    }

    /** Закрывает соединения. */
    fun close() = client.close()

    private fun HttpRequestBuilder.prepare(call: CabinetCall) {
        method = call.method
        headers.append(HttpHeaders.Accept, call.accept.toString())
        call.idempotencyKey?.let { headers.append(IDEMPOTENCY_KEY, it) }
        call.body?.let { setBody(TextContent(it, ContentType.Application.Json)) }
        identify(call.token)
    }

    /** Личность разработчика — заголовками, иначе доступ владельца. */
    private fun HttpRequestBuilder.identify(token: String?) {
        val identity = settings.development
        if (identity != null) {
            headers.append(DevelopmentIdentity.IIN_HEADER, identity.iin)
            headers.append(DevelopmentIdentity.BIN_HEADER, identity.bin)
        } else if (token != null) {
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    private suspend fun refusalOf(response: HttpResponse): CabinetRefusal {
        val text = response.bodyAsText()
        val error = runCatching { cabinetJson.decodeFromString<CabinetError>(text) }.getOrNull()
        return CabinetRefusal(
            code = error?.code ?: "HTTP_${response.status.value}",
            text = error?.text() ?: text.take(MAX_ERROR_LENGTH),
            httpStatus = response.status.value
        )
    }

    private companion object {
        const val IDEMPOTENCY_KEY = "Idempotency-Key"

        /** Сколько знаков неразобранного отказа доходит до приложения. */
        const val MAX_ERROR_LENGTH = 200
    }
}
