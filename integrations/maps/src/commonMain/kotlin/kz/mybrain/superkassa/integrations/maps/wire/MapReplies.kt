package kz.mybrain.superkassa.integrations.maps.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.integrations.maps.MapPlace
import kz.mybrain.superkassa.integrations.maps.MapPointPlace

/**
 * Разбор ответов служб карты.
 *
 * Своего в ответах мало: координаты строками, имена полей с подчёркиванием,
 * пара «широта,долгота» одной строкой. Разбор отделён от обращения, чтобы
 * проверяться записанными ответами служб, без сети.
 */

/** Найденное поиском по адресу. Неразборчивый ответ — `null`, как и молчание. */
internal fun placesOf(body: String): List<MapPlace>? =
    runCatching { json.decodeFromString<List<FoundPlace>>(body) }.getOrNull()?.mapNotNull(FoundPlace::place)

/** Город по ответу службы адреса подключения: `loc` — «широта,долгота». */
internal fun locationOf(body: String): MapPlace? {
    val answer = runCatching { json.decodeFromString<LocationAnswer>(body) }.getOrNull()
    val degrees = answer?.loc?.split(',')?.mapNotNull { it.trim().toDoubleOrNull() }?.takeIf { it.size == 2 }
    return degrees?.let { MapPlace(it[0], it[1], answer.city.orEmpty()) }
}

/**
 * Место под точкой по ответу службы.
 *
 * Пусто вместо места — не отказ: служба отвечает и на точку в степи,
 * только адреса в ответе нет. Место без улицы и без дома тоже разбирается:
 * по области с городом регистр доведёт владельца до улицы сам.
 */
internal fun pointPlaceOf(body: String): MapPointPlace? =
    runCatching { json.decodeFromString<ReverseAnswer>(body) }.getOrNull()?.address?.let(::placeOf)

/** Место по разобранному адресу; ни области, ни пункта — места нет. */
private fun placeOf(address: ReverseAddress): MapPointPlace? {
    val localities = address.localities()
    // У городов республиканского значения — Алматы, Астаны, Шымкента —
    // области нет: сам город стоит в регистре областью, и служба
    // не присылает ни `state`, ни `region`.
    val named = (address.state ?: address.region).orEmpty().trim()
    val region = named.ifBlank { localities.firstOrNull().orEmpty() }
    if (region.isBlank() && localities.isEmpty()) return null
    return MapPointPlace(
        region = region,
        localities = localities,
        street = address.road.orEmpty().trim(),
        house = address.houseNumber.orEmpty().trim()
    )
}

/** Найденное службой; координаты приходят строками. */
@Serializable
private data class FoundPlace(
    val lat: String? = null,
    val lon: String? = null,
    @SerialName("display_name") val displayName: String? = null
) {
    fun place(): MapPlace? {
        val latitude = lat?.toDoubleOrNull()
        val longitude = lon?.toDoubleOrNull()
        return if (latitude == null || longitude == null) null else MapPlace(latitude, longitude, displayName.orEmpty())
    }
}

/** Ответ службы адреса подключения; остальные её поля не читаются. */
@Serializable
private data class LocationAnswer(val loc: String? = null, val city: String? = null)

/** Ответ обратного поиска: всё нужное лежит в разобранном адресе. */
@Serializable
private data class ReverseAnswer(val address: ReverseAddress? = null)

/**
 * Разобранный адрес службы.
 *
 * Пункт приходит под одним из нескольких имён — у Алматы это `city`,
 * у села `village`, у района города `suburb` или `city_district`, —
 * поэтому берутся все, а не одно.
 */
@Serializable
private data class ReverseAddress(
    val state: String? = null,
    val region: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    @SerialName("city_district") val cityDistrict: String? = null,
    val suburb: String? = null,
    val road: String? = null,
    @SerialName("house_number") val houseNumber: String? = null
) {
    /** Пункты от крупного к мелкому, без повторов и пустых. */
    fun localities(): List<String> = listOfNotNull(city, town, village, municipality, county, cityDistrict, suburb)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private val json = Json { ignoreUnknownKeys = true }
