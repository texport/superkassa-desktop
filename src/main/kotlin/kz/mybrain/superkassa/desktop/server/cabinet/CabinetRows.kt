package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * Список из ответа кабинета, чем бы он ни был обёрнут.
 *
 * Кабинет отдаёт перечни по-разному: журнал регистрационных действий —
 * конвертом со страницей, версии карты — голым массивом, торговая
 * сводка — конвертом с именем раздела. Имя конверта частью договора
 * не считается: ошибка в нём однажды уже стоила владельцу окна
 * с текстом исключения разбора вместо списка версий карты.
 *
 * Берётся первый массив ответа; его нет — список пуст, и это не отказ.
 */
internal suspend inline fun <reified T> CabinetClient.rows(token: String, path: String): List<T> {
    val body: JsonElement = request(HttpMethod.Get, path, token = token)
    val rows = body as? JsonArray
        ?: (body as? JsonObject)?.values?.firstNotNullOfOrNull { it as? JsonArray }
        ?: return emptyList()
    return CabinetClient.lenientJson.decodeFromJsonElement(ListSerializer(serializer<T>()), rows)
}
