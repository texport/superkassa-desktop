package kz.mybrain.superkassa.integrations.bfdcabinet.transport

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart

/**
 * Одно обращение к кабинету: метод, путь и то, что к нему приложено.
 *
 * @property body тело запроса JSON-текстом; `null` — без тела.
 * @property token доступ владельца; `null` — обращение без входа.
 * @property idempotencyKey ключ повтора: запрос с тем же ключом кабинет
 *   не исполняет второй раз.
 * @property accept чего ждать в ответ — JSON или, например, PDF карты.
 */
internal class CabinetCall(
    val method: HttpMethod,
    val path: String,
    val body: String? = null,
    val token: String? = null,
    val idempotencyKey: String? = null,
    val accept: ContentType = ContentType.Application.Json
)

/** Значение для пути: идентификатор и код РКА — как есть, без склейки лишнего. */
internal fun String.inPath(): String = encodeURLPathPart()

/** Строка запроса из заданных параметров; незаданные не пишутся, пустая — без `?`. */
internal fun query(vararg parameters: Pair<String, Any?>): String {
    val written = parameters.mapNotNull { (name, value) ->
        value?.toString()?.takeIf { it.isNotBlank() }?.let { "$name=${it.encodeURLParameter(spaceToPlus = true)}" }
    }
    return if (written.isEmpty()) "" else written.joinToString("&", prefix = "?")
}
