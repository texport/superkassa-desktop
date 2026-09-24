package kz.mybrain.superkassa.presentation.analytics

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * Ответы кабинета для проверки раскладки аналитики.
 *
 * Два набора. Предельный собран здесь: миллиарды тенге, сто тысяч чеков,
 * девятнадцать областей длинными казахскими названиями, девяносто дней
 * на графике и пять тысяч касс на карте — раскладка обязана выдержать
 * их, а не данные показа. Живой — ответы кабинета владельца, снятые
 * GET-запросами в каталог [LIVE]; его в хранилище нет, и без каталога
 * живые кадры не снимаются.
 *
 * Ответ выбирается по пути запроса; незнакомый путь — отказ 404, как
 * у кабинета на ручку, которой нет.
 */
internal object AnalyticsFixtures {

    val LIVE = File("/tmp/adaptive-analytics-live")

    /** Живые ответы или `null`, если их не снимали. */
    fun live(): ((String) -> String?)? {
        if (!File(LIVE, "summary.json").exists() && !File(LIVE, "sales-summary.json").exists()) return null
        return { url -> liveFile(url)?.let { File(LIVE, it) }?.takeIf { it.exists() }?.readText() }
    }

    private fun liveFile(url: String): String? {
        val path = url.substringBefore('?')
        val page = Regex("page=(\\d+)").find(url)?.groupValues?.get(1)
        return when {
            path.endsWith("/cash-registers/map") -> "map-CABINET_COORDINATES.json"
            path.endsWith("/cash-registers/addresses") -> "addresses.json"
            path.contains("/api/analytics/sales/") -> "sales-${path.substringAfterLast('/')}.json"
            path == "/api/retail-places" -> "places-${page ?: "0"}.json"
            else -> null
        }
    }

    /** Предельный набор. */
    fun extreme(): (String) -> String? {
        val map = extremeMap().toString()
        val places = extremePlaces()
        val registers = AnalyticsExtremeSales.registers().toString()
        val placeUnits = AnalyticsExtremeSales.places().toString()
        val addresses = extremeAddresses().toString()
        return { url ->
            val path = url.substringBefore('?')
            val page = Regex("page=(\\d+)").find(url)?.groupValues?.get(1)?.toInt() ?: 0
            when {
                path.endsWith("/cash-registers/map") -> map
                path.endsWith("/cash-registers/addresses") -> addresses
                path.endsWith("/sales/summary") -> AnalyticsExtremeSales.summary()
                path.endsWith("/sales/by-day") -> AnalyticsExtremeSales.days()
                path.endsWith("/sales/by-hour") -> AnalyticsExtremeSales.hours()
                path.endsWith("/sales/by-cash-register") -> registers
                path.endsWith("/sales/by-retail-place") -> placeUnits
                path.endsWith("/sales/documents") -> AnalyticsExtremeSales.documents()
                path == "/api/retail-places" -> placesPage(places, page)
                else -> null
            }
        }
    }

    /** Девятнадцать областей: самые длинные названия — казахские. */
    val REGIONS = listOf(
        "Солтүстік Қазақстан облысы", "Шығыс Қазақстан облысы", "Батыс Қазақстан облысы",
        "Қарағанды облысы", "Маңғыстау облысы", "Түркістан облысы", "Қызылорда облысы",
        "Павлодар облысы", "Қостанай облысы", "Ақмола облысы", "Ақтөбе облысы", "Атырау облысы",
        "Жамбыл облысы", "Алматы облысы", "Жетісу облысы", "Ұлытау облысы", "Абай облысы",
        "Астана қаласы", "Алматы қаласы"
    )

    const val KKMS = 5000
    const val PLACES = 2000
    private val STATUSES = listOf("REGISTERED", "DRAFT", "REGISTRATION_IN_ISNA_PROCESS", "DEREGISTERED", "REJECTED")

    private fun region(place: Int) = REGIONS[place % REGIONS.size]

    fun placeName(place: Int) = "Сауда орталығы «Бәйтерек-Нұр» № ${place + 1}"

    private fun address(place: Int) = "${region(place)}, Мағжан Жұмабаев ауданы, Тәуелсіздік даңғылы, ${place + 1}"

    /** Место на карте: точки сеткой пятьдесят на сорок по всей стране. */
    private fun latitude(place: Int) = 41.5 + (place % 50) * 0.24

    private fun longitude(place: Int) = 51.0 + (place / 50) * 0.85

    private fun kkm(index: Int): JsonObject = buildJsonObject {
        val place = index % PLACES
        put("cashRegisterId", "kkm-$index")
        put("kkmId", 5_000_000 + index)
        put("registrationNumber", "2609400${"%05d".format(index)}")
        put("internalName", "Касса № ${index + 1} — кіреберістегі үлкен")
        put("retailPlaceId", "place-$place")
        put("retailPlaceName", placeName(place))
        put("address", address(place))
        put("status", STATUSES[index % STATUSES.size])
        put("blocked", index % 97 == 0)
        put("shiftStatus", if (index % 2 == 0) "OPEN" else "CLOSED")
        put("shiftNumber", index % 400 + 1)
        put("lastContactAt", "2026-09-2${index % 3 + 1}T10:1${index % 10}:00Z")
        put(
            "position",
            buildJsonObject {
                put("source", "CABINET_COORDINATES")
                put("latitude", latitude(place))
                put("longitude", longitude(place))
            }
        )
    }

    private fun extremeMap(): JsonObject = buildJsonObject {
        put("positionSource", "CABINET_COORDINATES")
        put("placedCount", KKMS)
        put("withoutPositionCount", 0)
        put("placed", JsonArray((0 until KKMS).map(::kkm)))
        put("withoutPosition", JsonArray(emptyList()))
    }

    private fun extremePlaces(): List<JsonObject> = (0 until PLACES).map { place ->
        buildJsonObject {
            put("id", "place-$place")
            put("name", placeName(place))
            put("address", address(place))
            put("addressKk", address(place))
            put("latitude", latitude(place))
            put("longitude", longitude(place))
            put("cashRegisterCount", KKMS / PLACES)
        }
    }

    private fun placesPage(places: List<JsonObject>, page: Int): String = buildJsonObject {
        put("page", page)
        put("size", PAGE)
        put("totalElements", places.size)
        put("items", JsonArray(places.drop(page * PAGE).take(PAGE)))
    }.toString()

    private const val PAGE = 100

    /** Адреса обмена: длинные IPv6 и длинные названия касс. */
    private fun extremeAddresses(): JsonObject = buildJsonObject {
        val rows = (0 until 400).map { index ->
            buildJsonObject {
                put("cashRegisterId", "kkm-${index % 150}")
                put("kkmId", 5_000_000 + index % 150)
                put("registrationNumber", "2609400${"%05d".format(index % 150)}")
                put("internalName", "Касса № ${index % 150 + 1} — кіреберістегі үлкен")
                put("retailPlaceName", placeName(index % 150))
                put("address", "2a02:2168:a0f:${"%04x".format(index)}:9c5d:7fff:fe12:${"%04x".format(index)}")
                put("firstSeen", "2026-09-01T07:47:51Z")
                put("lastSeen", "2026-09-22T20:29:40Z")
            }
        }
        put("cashRegisterCount", 150)
        put("addressCount", rows.size)
        put("addresses", JsonArray(rows))
    }
}
