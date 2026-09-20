package kz.mybrain.superkassa.desktop.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

/**
 * Что за место под меткой, словами картографической службы.
 *
 * Названия здесь чужие: их дала карта, а не государственный регистр.
 * Адресом точки ни одно из них не становится — по ним только отыскивается
 * запись регистра (см. [matchPointPlace]), и в точку уходит найденное там.
 *
 * @param localities пункты от крупного к мелкому: город, потом район.
 *   Регистр держит улицы то под городом, то под районом, и знать заранее,
 *   какой из них его, приложение не может.
 */
data class MapPointPlace(
    val region: String,
    val localities: List<String>,
    val street: String,
    val house: String
)

/**
 * Координаты в место: обратное геокодирование.
 *
 * Прямой поиск (см. [MapGeocoder]) ведёт карту к выбранному в регистре
 * адресу; здесь обратный ход — владелец ставит метку, а служба говорит,
 * что это за место. Регистр кабинета такого не умеет вовсе: по коду РКА
 * он отдаёт адрес, но по точке — ничего, координат у него нет.
 *
 * **Про службу.** Та же открытая служба сообщества, что и у поиска, и то
 * же правило: называть себя, спрашивать по нажатию владельца, а не за
 * каждым движением карты. Перед выпуском на всех владельцев заменяется
 * своей или оплаченной — адрес вынесен в [service].
 */
class MapReverseGeocoder(val service: String = MapService.REVERSE) {

    /** Место под точкой. `null` — служба не ответила или места не знает. */
    suspend fun at(latitude: Double, longitude: Double): MapPointPlace? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$service?format=jsonv2&zoom=$HOUSE_DETAIL&accept-language=ru&lat=$latitude&lon=$longitude"
            val connection = URI.create(url).toURL().openConnection()
            connection.setRequestProperty("User-Agent", MapService.AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            pointPlaceOf(connection.getInputStream().use { it.reader().readText() })
        }.getOrNull()
    }

    private companion object {
        const val TIMEOUT_MS = 8_000

        /** Подробность ответа: дом. Крупнее — служба отвечает улицей или кварталом. */
        const val HOUSE_DETAIL = 18
    }
}

/**
 * Разбор ответа службы.
 *
 * Пусто вместо места — не отказ: служба отвечает и на точку в степи,
 * только адреса в ответе нет. Место без улицы и без дома тоже приходит
 * и тоже разбирается: по области с городом регистр доведёт владельца
 * до улицы сам, а молчание вместо ответа не объяснило бы ничего.
 */
internal fun pointPlaceOf(body: String): MapPointPlace? {
    val address = runCatching { json.decodeFromString<ReverseAnswer>(body) }.getOrNull()?.address ?: return null
    val localities = listOfNotNull(
        address.city,
        address.town,
        address.village,
        address.municipality,
        address.county,
        address.cityDistrict,
        address.suburb
    ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    // У городов республиканского значения — Алматы, Астаны, Шымкента —
    // области нет вовсе: сам город и стоит в регистре областью. Служба
    // в таком ответе не присылает ни `state`, ни `region`, и подбор
    // спотыкался на первом же шаге: «в регистре нет области».
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

/** Ответ службы: всё нужное лежит в разобранном адресе. */
@Serializable
private data class ReverseAnswer(val address: ReverseAddress? = null)

/**
 * Разобранный адрес службы.
 *
 * Полей у него больше, чем здесь: взяты те, что отвечают шагам регистра.
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
)

private val json = Json { ignoreUnknownKeys = true }
