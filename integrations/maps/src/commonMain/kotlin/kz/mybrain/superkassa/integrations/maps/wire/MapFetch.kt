package kz.mybrain.superkassa.integrations.maps.wire

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kz.mybrain.superkassa.integrations.maps.MapJournal
import kotlin.time.Duration

/**
 * Одно обращение к службе карт — одна копия на все четыре службы.
 *
 * Неудача пишется в журнал именем службы и помехи, без адреса и координат.
 * Неудача — любая помеха, кроме отмены: и оборванная связь, и адрес,
 * которого движок не принимает; тело читается здесь же, чтобы и обрыв
 * посреди ответа был той же неудачей, а не исключением у вызывающего.
 * Ответ не тем кодом — тоже неудача: страницу ошибки прокси разбирать
 * как ответ службы нельзя.
 *
 * @return ответ; `null` — служба не ответила или ответила отказом.
 */
internal class MapFetch(
    private val http: HttpClient,
    private val journal: MapJournal,
    private val userAgent: () -> String
) {
    suspend fun get(service: String, url: String, wait: Duration, query: HttpRequestBuilder.() -> Unit = {}): Answer? {
        val answer = try {
            val response = http.get(url) {
                headers.append(HttpHeaders.UserAgent, userAgent())
                timeout { requestTimeoutMillis = wait.inWholeMilliseconds }
                query()
            }
            Answer(response.status, response.headers[HttpHeaders.ContentType].orEmpty(), response.bodyAsBytes())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: IOException) {
            silence(service, failure)
        } catch (failure: IllegalArgumentException) {
            // Не только сеть: адрес не той схемы движок отвергает этим
            // исключением, а для владельца это та же неответившая служба.
            silence(service, failure)
        } catch (failure: IllegalStateException) {
            // Адрес, который не разобрать, Ktor отвергает этим исключением.
            silence(service, failure)
        }
        if (answer != null && !answer.status.isSuccess()) {
            journal.failed(service, "HTTP ${answer.status.value}")
        }
        return answer?.takeIf { it.status.isSuccess() }
    }

    private fun silence(service: String, failure: Exception): Answer? {
        journal.failed(service, failure::class.simpleName.orEmpty())
        return null
    }

    /** Ответ, прочитанный целиком, и его вид: плитку без вида картинки в хранилище не кладут. */
    class Answer(val status: HttpStatusCode, val contentType: String, val bytes: ByteArray) {
        fun text(): String = bytes.decodeToString()
    }
}
