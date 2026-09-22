package kz.mybrain.superkassa.desktop.server

import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent

/**
 * Остальные обращения к узлу: номенклатура, печатные формы и повторная
 * отправка документов.
 */
/**
 * Позиция справочника по штрихкоду или `null`, если такой позиции в нём нет.
 *
 * `null` означает ровно одно: справочник ответил, и товара у него нет.
 * Всё остальное — молчание узла, недоступный БФД, заблокированная касса —
 * поднимается отказом. Прежде узел отвечал на эти беды тем же 404, и касса
 * писала кассиру «нет такого штрихкода» о заведённом товаре.
 */
suspend fun ServerClient.lookupBarcode(kkmId: String, barcode: String, pin: String): NomenclatureItem? {
    val response = call(HttpMethod.Get, "/kkm/$kkmId/nomenclature/lookup?barcode=$barcode", null, pin)
    if (response.status.value !in SUCCESS_RANGE) {
        val refusal = refusalOf(response)
        if (refusal.httpStatus == NOT_FOUND && refusal.code in ABSENT_CODES) return null
        throw refusal
    }
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
 * Печатная форма документа, переданного данными.
 *
 * Узел рисует и такой документ: касса-рисовальщик не обязана быть той,
 * что его пробила. Данные — пакет протокола, запрос кассы и ответ ОФД;
 * это всё, что о документе известно вне той кассы, и этого рисовальщику
 * хватает. Свой рисунок в приложении завёл бы второй вид того же
 * документа — здесь нет ни рисовальщика, ни браузера.
 *
 * @param packet пакет протокола целиком, как его отдал кабинет.
 * @param kind что запросить у узла.
 */
suspend fun ServerClient.printPacket(
    kkmId: String,
    packet: String,
    pin: String,
    kind: PrintKind
): ByteArray {
    val response = callAccepting(
        HttpMethod.Post,
        "/kkm/$kkmId/documents/print.${kind.extension}",
        kind.contentType,
        pin,
        // Пакет уходит как есть: разбирать и собирать его заново значило бы
        // держать в приложении своё представление протокола, которого здесь
        // быть не должно.
        TextContent(packet, ContentType.Application.Json)
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

/**
 * Чем узел называет отсутствие товара в справочнике.
 *
 * Два кода, потому что узел прежних выпусков отвечал общим `NOT_FOUND`:
 * с ним касса продолжает работать, пока он не обновлён. Ненайденная касса
 * отвечает своим кодом и сюда не попадает — иначе её отсутствие читалось бы
 * как отсутствие товара.
 */
private val ABSENT_CODES = setOf("NOMENCLATURE_NOT_FOUND", "NOT_FOUND")
