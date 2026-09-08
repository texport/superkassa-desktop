package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod

/**
 * Чтение состояния кассы: журнал документов, счётчики и очередь.
 */
suspend fun ServerClient.shiftDocuments(kkmId: String, pin: String): List<Document> =
    request(HttpMethod.Get, "/kkm/$kkmId/shift/documents?limit=200", pin = pin)

/**
 * Документы за период.
 *
 * Узел отдаёт их страницами: раньше приложение брало первые двести
 * и молчало об остальных — за оживлённый день журнал показывал часть дня,
 * не сообщая, что она часть.
 */
suspend fun ServerClient.documents(
    kkmId: String,
    fromMillis: Long,
    toMillis: Long,
    pin: String,
    offset: Int = 0,
    limit: Int = PAGE
): List<Document> =
    request(
        HttpMethod.Get,
        "/kkm/$kkmId/documents?from=$fromMillis&to=$toMillis&limit=$limit&offset=$offset",
        pin = pin
    )

suspend fun ServerClient.counters(kkmId: String, pin: String): List<CounterRecord> =
    request(HttpMethod.Get, "/kkm/$kkmId/counters", pin = pin)

suspend fun ServerClient.queue(kkmId: String, pin: String): List<QueueTask> =
    request(HttpMethod.Get, "/kkm/$kkmId/queue", pin = pin)

/** Сколько документов узел отдаёт за одно обращение. */
const val PAGE: Int = 200
