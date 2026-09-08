package kz.mybrain.superkassa.desktop.server

import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod

/**
 * Остальные обращения к узлу: номенклатура, печатные формы и повторная
 * отправка документов.
 */
suspend fun ServerClient.lookupBarcode(kkmId: String, barcode: String, pin: String): NomenclatureItem? {
    val response = call(HttpMethod.Get, "/kkm/$kkmId/nomenclature/lookup?barcode=$barcode", null, pin)
    if (response.status.value == NOT_FOUND) return null
    if (response.status.value !in SUCCESS_RANGE) throw refusalOf(response)
    val found = ServerClient.lenientJson.decodeFromString(
        NomenclatureLookup.serializer(),
        response.readRawBytes().decodeToString()
    )
    return found.item?.takeIf { found.found }
}

/**
 * Печатная форма документа в виде картинки.
 *
 * Запрашивается именно картинка: со стандартным `Accept: application/json`
 * узел на этот путь отвечает ошибкой, и кассир видел бы нажатую кнопку
 * без всякого действия. Отказ поднимается наружу по той же причине:
 * молчание на нажатие — худший из ответов.
 */
suspend fun ServerClient.printImage(kkmId: String, documentId: String, pin: String): ByteArray {
    val response = callAccepting(
        HttpMethod.Get,
        "/kkm/$kkmId/documents/$documentId/print.png",
        ContentType.Image.PNG,
        pin
    )
    if (response.status.value !in SUCCESS_RANGE) throw refusalOf(response)
    return response.readRawBytes()
}

/** Повторная отправка всех неудачных задач очереди. */
suspend fun ServerClient.retryFailedQueue(kkmId: String, pin: String) {
    val response = call(HttpMethod.Post, "/kkm/$kkmId/queue/retry-failed", null, pin)
    if (response.status.value !in SUCCESS_RANGE) throw refusalOf(response)
}

/** Сведения об ОФД: адрес, окружение, версия протокола. */
suspend fun ServerClient.ofdInfo(kkmId: String, pin: String): Map<String, String> {
    val response = call(HttpMethod.Get, "/kkm/$kkmId/ofd/info", null, pin)
    if (response.status.value !in SUCCESS_RANGE) throw refusalOf(response)
    val text = response.readRawBytes().decodeToString()
    val parsed = runCatching {
        ServerClient.lenientJson.parseToJsonElement(text)
    }.getOrNull() ?: return emptyMap()
    return parsed.toStringMap()
}

/**
 * Печатная форма документа в выбранном виде.
 *
 * Узел рисует одну и ту же форму в разных представлениях: картинкой для
 * экрана и принтера, PDF и HTML — для передачи покупателю и в бухгалтерию.
 * Рисует всегда узел: свой рисунок дал бы два разных чека по одному
 * документу.
 *
 * @param kind что запросить у узла.
 */
suspend fun ServerClient.printDocument(
    kkmId: String,
    documentId: String,
    pin: String,
    kind: PrintKind
): ByteArray {
    val response = callAccepting(
        HttpMethod.Get,
        "/kkm/$kkmId/documents/$documentId/print.${kind.extension}",
        kind.contentType,
        pin
    )
    if (response.status.value !in SUCCESS_RANGE) throw refusalOf(response)
    return response.readRawBytes()
}

/**
 * Документ с составом чека и тем, кто его оформил.
 *
 * Возврату нужны позиции: вернуть можно то, что продано, и в том
 * количестве, в каком продано. Разбору отказа ОФД нужен кассир —
 * в списке документов виден только код.
 */
suspend fun ServerClient.documentDetails(kkmId: String, documentId: String, pin: String): DocumentDetails {
    val response = call(HttpMethod.Get, "/kkm/$kkmId/documents/$documentId", null, pin)
    if (response.status.value !in SUCCESS_RANGE) throw refusalOf(response)
    return ServerClient.lenientJson.decodeFromString(
        DocumentDetails.serializer(),
        response.readRawBytes().decodeToString()
    )
}

/** Виды печатной формы, которые отдаёт узел. */
enum class PrintKind(val extension: String, val contentType: ContentType) {
    Png("png", ContentType.Image.PNG),
    Pdf("pdf", ContentType.Application.Pdf),
    Html("html", ContentType.Text.Html)
}

private const val NOT_FOUND = 404
private val SUCCESS_RANGE = 200..299
