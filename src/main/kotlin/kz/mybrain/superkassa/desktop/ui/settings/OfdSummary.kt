package kz.mybrain.superkassa.desktop.ui.settings

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.strings.KkmSetupTexts

/**
 * Ответ ОФД, сведённый к тому, что кассиру полезно.
 *
 * Узел отдаёт на этот запрос весь протокольный обмен: двоичный пакет
 * целиком, заголовок с токеном и Z-отчёт. Показывать это на экране нельзя
 * ни по смыслу — кассир не читает протокол, — ни по правилу: токен и полные
 * пакеты не место на экране кассы.
 */
data class OfdSummary(
    val answer: String? = null,
    val protocol: String? = null,
    val kgdNumber: String? = null,
    val factoryNumber: String? = null,
    val systemId: String? = null,
    val organization: String? = null,
    val address: String? = null,
    val bin: String? = null
) {
    /** Подписанные строки для показа. Пустые поля не занимают места. */
    fun rows(texts: KkmSetupTexts): List<Pair<String, String>> = listOfNotNull(
        answer?.let { texts.ofdAnswer to it },
        organization?.let { texts.ofdOrganization to it },
        address?.let { texts.ofdAddress to it },
        bin?.let { texts.ofdBin to it },
        kgdNumber?.let { texts.ofdKgdNumber to it },
        factoryNumber?.let { texts.ofdFactoryNumber to it },
        systemId?.let { texts.ofdSystemId to it },
        protocol?.let { texts.ofdProtocol to it }
    )
}

/**
 * Разбирает ответ узла на запрос сведений об ОФД.
 *
 * Неразобранный ответ — это пустая сводка, а не сбой: узел мог ответить
 * иначе, и падать на экране диагностики из-за этого нельзя.
 */
fun parseOfdInfo(body: String, language: String): OfdSummary {
    val root = runCatching { ServerClient.lenientJson.parseToJsonElement(body) }.getOrNull()
    val response = root.child("responseJson")
    val payload = response.child("payload")
    val kkm = payload.child("service").child("regInfo").child("kkm")
    val org = payload.child("service").child("regInfo").child("org")
    val result = payload.child("result").child("resultType")
    return OfdSummary(
        answer = result.child(descriptionKey(language)).text() ?: result.child("descriptionRu").text(),
        protocol = response.child("protocolVersion").text(),
        kgdNumber = kkm.child("fnsKkmId").text(),
        factoryNumber = kkm.child("serialNumber").text(),
        systemId = kkm.child("kkmId").text(),
        organization = org.child("title").text(),
        address = if (language == KAZAKH) {
            org.child("addressKz").text() ?: org.child("address").text()
        } else {
            org.child("address").text()
        },
        bin = org.child("inn").text()
    )
}

/**
 * Сведения об узле, показываемые кассиру.
 *
 * Полный ответ узла на этот запрос содержит и пины по умолчанию, и путь
 * к базе. Ни то, ни другое на экран не выводится: пин — право на фискальную
 * команду, и место ему в памяти, а не в интерфейсе.
 */
data class NodeFacts(
    val mode: String? = null,
    val protocol: String? = null,
    val timeoutSeconds: String? = null,
    val storage: String? = null
) {
    fun rows(texts: KkmSetupTexts): List<Pair<String, String>> = listOfNotNull(
        mode?.let { texts.nodeMode to it },
        protocol?.let { texts.nodeProtocol to it },
        timeoutSeconds?.let { texts.nodeTimeout to it },
        storage?.let { texts.nodeStorage to it }
    )
}

fun parseNodeSettings(body: String): NodeFacts {
    val root = runCatching { ServerClient.lenientJson.parseToJsonElement(body) }.getOrNull()
    return NodeFacts(
        mode = root.child("mode").text(),
        protocol = root.child("ofdProtocolVersion").text(),
        timeoutSeconds = root.child("ofdTimeoutSeconds").text(),
        storage = root.child("storage").child("engine").text()
    )
}

private fun descriptionKey(language: String): String = when (language) {
    KAZAKH -> "descriptionKz"
    ENGLISH -> "descriptionEn"
    else -> "descriptionRu"
}

private fun JsonElement?.child(name: String): JsonElement? = (this as? JsonObject)?.get(name)

private fun JsonElement?.text(): String? =
    (this as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

private const val KAZAKH = "kk"
private const val ENGLISH = "en"
