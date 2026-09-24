package kz.mybrain.superkassa.domain.settings.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Ответ ОФД, сведённый к тому, что кассиру полезно.
 *
 * Касса отдаёт на этот запрос весь протокольный обмен: двоичный пакет
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
    companion object {
        /**
         * Сводка ответа ОФД на запрос сведений о кассе.
         *
         * Неразобранный ответ — пустая сводка, а не сбой: ОФД мог ответить
         * иначе, и падать на экране диагностики из-за этого нельзя.
         *
         * @param response разобранный ответ ОФД, как его отдала касса.
         * @param language код языка кассира: `kk`, `ru` или `en`.
         */
        fun of(response: JsonObject?, language: String): OfdSummary = parse(response, language)
    }
}

/**
 * Разбор ответа ОФД по полям протокола.
 *
 * Имена полей — протокольные: `fnsKkmId` и `inn` так названы в ответе
 * ОФД, а на экране это регистрационный номер КГД и БИН.
 */
private fun parse(response: JsonObject?, language: String): OfdSummary {
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
