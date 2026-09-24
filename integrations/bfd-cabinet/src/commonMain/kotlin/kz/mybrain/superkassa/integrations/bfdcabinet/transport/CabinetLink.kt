package kz.mybrain.superkassa.integrations.bfdcabinet.transport

import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kz.mybrain.superkassa.integrations.bfdcabinet.signin.CabinetAccess

/**
 * Ручки кабинета от имени вошедшего: путь, тело и тип ответа.
 *
 * Сценарии называют действие словами предметной области и пишут путь один
 * раз; доступ, отказы и разбор — здесь.
 */
internal class CabinetLink(val http: CabinetHttp, val access: CabinetAccess) {

    /** Чтение: ответ в тип [T]. */
    suspend inline fun <reified T> get(path: String): T =
        access.call { token -> decoded<T>(path, text(CabinetCall(HttpMethod.Get, path, token = token))) }

    /** Изменение с телом [body] и ответом типа [T]. */
    suspend inline fun <reified B, reified T> send(method: HttpMethod, path: String, body: B, key: String? = null): T =
        access.call { token ->
            val call = CabinetCall(method, path, encoded(body), token, idempotencyKey = key)
            decoded<T>(path, text(call))
        }

    /** Действие без тела с ответом типа [T]. */
    suspend inline fun <reified T> post(path: String, key: String? = null): T =
        access.call { token ->
            decoded<T>(path, text(CabinetCall(HttpMethod.Post, path, token = token, idempotencyKey = key)))
        }

    /** Действие, ответ на которое не читается. */
    suspend fun done(method: HttpMethod, path: String) {
        access.call { token -> http.send(CabinetCall(method, path, token = token)) }
    }

    /** Ответ не в JSON — например, PDF регистрационной карты. */
    suspend fun bytes(path: String, accept: ContentType): ByteArray = access.call { token ->
        http.send(CabinetCall(HttpMethod.Get, path, token = token, accept = accept)).bodyAsBytes()
    }

    /** Ответ текстом — для разбора. */
    suspend fun text(call: CabinetCall): String = http.send(call).bodyAsText()

    /**
     * Список из ответа, чем бы он ни был обёрнут.
     *
     * Кабинет отдаёт перечни по-разному: голым массивом, конвертом со
     * страницей, конвертом с именем раздела. Имя конверта частью договора
     * не считается: берётся первый массив ответа, а нет его — список пуст.
     */
    suspend inline fun <reified T> rows(path: String): List<T> {
        val body: JsonElement = get(path)
        val rows = body as? JsonArray
            ?: (body as? JsonObject)?.values?.firstNotNullOfOrNull { it as? JsonArray }
            ?: return emptyList()
        return decoded(path) { cabinetJson.decodeFromJsonElement(ListSerializer(serializer<T>()), rows) }
    }
}
