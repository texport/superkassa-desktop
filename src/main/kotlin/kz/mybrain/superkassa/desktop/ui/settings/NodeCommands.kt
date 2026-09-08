package kz.mybrain.superkassa.desktop.ui.settings

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.desktop.server.ServerClient

/**
 * Обращения узла, которыми пользуются только настройки.
 *
 * Сверка с ОФД, снятие кассы с учёта и сведения об узле нужны одному
 * экрану и вызываются раз в жизни кассы. Общему клиенту от них ни тепло,
 * ни холодно, а держать их рядом с местом применения — короче на одно
 * путешествие по дереву.
 */

/** Сверяет сведения о кассе с ОФД: организация, адрес, номера. */
internal suspend fun ServerClient.syncOfdServiceInfo(kkmId: String, pin: String) =
    post("/kkm/$kkmId/ofd/sync", pin)

/** Забирает из ОФД счётчики и номер смены. */
internal suspend fun ServerClient.syncOfdCounters(kkmId: String, pin: String) =
    post("/kkm/$kkmId/ofd/counters/sync", pin)

/**
 * Заменяет токен ОФД.
 *
 * Тело запроса — один токен: узел разбирает его сам и снимает блокировку,
 * наложенную по коду «неверный токен».
 */
internal suspend fun ServerClient.updateOfdToken(kkmId: String, token: String, pin: String) {
    val response = call(HttpMethod.Put, "/kkm/$kkmId/ofd/token", TokenUpdate(token), pin)
    if (!response.status.isSuccess()) throw refusalOf(response)
}

/** Тело запроса замены токена. */
@Serializable
private data class TokenUpdate(val token: String)

/** Снимает кассу с учёта: узел удаляет её вместе с документами. */
internal suspend fun ServerClient.decommissionKkm(kkmId: String, pin: String) {
    val response = call(HttpMethod.Delete, "/kkm/$kkmId", null, pin)
    if (!response.status.isSuccess()) throw refusalOf(response)
}

/** Ответ ОФД на запрос сведений — как есть, разбирает его [parseOfdInfo]. */
internal suspend fun ServerClient.ofdInfoBody(kkmId: String, pin: String): String {
    val response = call(HttpMethod.Get, "/kkm/$kkmId/ofd/info", null, pin)
    if (!response.status.isSuccess()) throw refusalOf(response)
    return response.bodyAsText()
}

/** Настройки узла — разбирает их [parseNodeSettings]. */
internal suspend fun ServerClient.nodeSettingsBody(): String {
    val response = call(HttpMethod.Get, "/settings", null, null)
    if (!response.status.isSuccess()) throw refusalOf(response)
    return response.bodyAsText()
}

private suspend fun ServerClient.post(path: String, pin: String) {
    val response = call(HttpMethod.Post, path, null, pin)
    if (!response.status.isSuccess()) throw refusalOf(response)
}
