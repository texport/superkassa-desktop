package kz.mybrain.superkassa.integrations.egovmobile

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Сеть посредника: сроки, разбор и журнал.
 *
 * Отказ отделяется от молчания здесь: ответ не успехом —
 * [EgovReason.Refused], ответа нет — [EgovReason.Unreachable].
 */
internal class EgovHttp(settings: EgovSettings, private val journal: EgovJournal, engine: HttpClientEngine) {
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = settings.answerWait.inWholeMilliseconds
            requestTimeoutMillis = settings.answerWait.inWholeMilliseconds
            socketTimeoutMillis = settings.answerWait.inWholeMilliseconds
        }
    }

    /**
     * POST одного шага процедуры: JSON туда, текст ответа обратно; [step] — имя шага для журнала.
     *
     * @param wait сколько ждать ответа, когда посредник держит запрос дольше
     *   обычного — до шага владельца в eGov mobile; `null` — обычный срок.
     */
    suspend fun post(url: String, json: String, step: String, wait: Duration? = null): String {
        val started = TimeSource.Monotonic.markNow()
        val response = try {
            client.post(url) {
                setBody(TextContent(json, ContentType.Application.Json))
                wait?.let { held(it) }
            }
        } catch (failure: IOException) {
            journal.record("$step failed", failure)
            throw EgovRefusal(EgovReason.Unreachable, failure::class.simpleName.orEmpty(), failure)
        }
        journal.record("$step -> ${response.status.value} in ${started.elapsedNow().inWholeMilliseconds} ms", null)
        return textOf(response)
    }

    /**
     * Долгий запрос подписи: посредник отвечает, когда подпись есть.
     *
     * @return текст ответа посредника; неудача — запрос оборвался или
     *   не дождался, и его можно повторить.
     */
    suspend fun poll(url: String, wait: Duration): Result<String> {
        val response = try {
            client.get(url) { held(wait) }
        } catch (failure: IOException) {
            journal.record("await signature interrupted", failure)
            return Result.failure(failure)
        }
        journal.record("await signature -> ${response.status.value}", null)
        return Result.success(textOf(response))
    }

/**
     * Запрос, который посредник держит, пока владелец не отзовётся в eGov
     * mobile: весь этот срок по соединению не идёт ни байта. Поэтому срок
     * тишины сокета — тот же, что у запроса: общий срок ответа в полминуты
     * обрывал удержанный запрос, пока владелец ещё сканировал QR, и вход
     * кончался словами «служба подписи не отвечает».
     */
    private fun HttpRequestBuilder.held(wait: Duration) = timeout {
        val millis = wait.inWholeMilliseconds.coerceAtLeast(1)
        requestTimeoutMillis = millis
        socketTimeoutMillis = millis
    }

    fun close() = client.close()

    private suspend fun textOf(response: HttpResponse): String {
        if (!response.status.isSuccess()) throw EgovRefusal(EgovReason.Refused, "HTTP ${response.status.value}")
        return response.bodyAsText()
    }
}
