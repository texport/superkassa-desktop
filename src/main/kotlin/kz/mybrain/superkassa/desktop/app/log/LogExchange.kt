package kz.mybrain.superkassa.desktop.app.log

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import java.io.IOException

/**
 * Обращение к службе, записанное в журнал целиком.
 *
 * Обёртка стоит в одном месте — там, где запрос уходит, — а не у каждого
 * вызова: обращений к узлу и к кабинету под сотню, и записывать их
 * поимённо означает забыть половину.
 *
 * Записывается метод, путь, код ответа и время: по ним видно, отвечает ли
 * служба и что именно она отвергла. Тела уходят в журнал только
 * на отладочном уровне — и вырезанными, см. [hideSecrets].
 *
 * @param body тело запроса как есть; в журнал попадёт без тайного.
 * @param call само обращение.
 */
suspend fun logged(
    source: LogSource,
    method: HttpMethod,
    path: String,
    body: Any?,
    call: suspend () -> HttpResponse
): HttpResponse {
    val started = System.nanoTime()
    val response = try {
        call()
    } catch (failure: IOException) {
        val reason = failure.message ?: failure::class.simpleName.orEmpty()
        AppLog.record(source, LogLevel.Failure, "${method.value} $path — нет ответа: $reason")
        throw failure
    }
    val status = response.status.value
    AppLog.record(
        source = source,
        level = levelOf(status),
        text = "${method.value} $path -> $status за ${millisSince(started)} мс",
        body = exchangeBody(path, body, response)
    )
    return response
}

/**
 * Насколько важен ответ.
 *
 * Отказ по существу — предупреждение: узел ответил, и кассиру уже сказано
 * его словами. Сбой службы — отказ: разбираться с ним обслуживанию.
 */
private fun levelOf(status: Int): LogLevel = when {
    status < HTTP_REFUSAL -> LogLevel.Info
    status < HTTP_BROKEN -> LogLevel.Warning
    else -> LogLevel.Failure
}

/** Что ушло и что вернулось — одной записью, только на отладочном уровне. */
private suspend fun exchangeBody(path: String, request: Any?, response: HttpResponse): String? {
    if (AppLog.level != LogLevel.Debug) return null
    if (hiddenAnswer(path)) return "ответ не записан: адреса обмена касс"

    val sent = request?.let { "-> $it" }
    val received = response.textForLog()?.let { "<- $it" }
    return listOfNotNull(sent, received).joinToString("\n    ").takeIf { it.isNotEmpty() }
}

/**
 * Ответ, который не пишется в журнал ни на каком уровне.
 *
 * Адреса, с которых кассы выходят на связь, — сведение о сети владельца.
 * Журнал он отправляет в поддержку, и вместе с отказом туда уезжал бы
 * список адресов всего парка. Ответ виден на экране аналитики и там же
 * разбирается.
 */
private fun hiddenAnswer(path: String) = path.substringBefore(QUERY).endsWith(ADDRESSES)

private const val QUERY = '?'

/** Хвост пути, отвечающего адресами обмена. */
private const val ADDRESSES = "/addresses"

/**
 * Тело ответа в виде, пригодном для журнала.
 *
 * Картинка печатной формы и PDF регистрационной карты в журнал не идут:
 * это мегабайты двоичного, из которых при разборе отказа не читается
 * ничего. От них остаётся строка о том, что ответ пришёл и чем он был.
 */
private suspend fun HttpResponse.textForLog(): String? {
    val type = contentType()
    val readable = type == null ||
        type.match(ContentType.Application.Json) ||
        type.match(ContentType.Text.Any)
    if (!readable) return "$type — тело не записано"
    return runCatching { bodyAsText() }.getOrNull()
}

private fun millisSince(started: Long) = (System.nanoTime() - started) / NANOS_IN_MILLI

/** Ответ от этого кода — отказ по существу. */
private const val HTTP_REFUSAL = 400

/** Ответ от этого кода — сбой самой службы. */
private const val HTTP_BROKEN = 500

private const val NANOS_IN_MILLI = 1_000_000
