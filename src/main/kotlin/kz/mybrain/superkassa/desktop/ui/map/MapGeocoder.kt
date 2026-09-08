package kz.mybrain.superkassa.desktop.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder

/**
 * Адрес в координаты.
 *
 * Это и есть настоящий ответ на вопрос «где стоит точка». Определение
 * по адресу подключения даёт город — да и то город поставщика связи,
 * а не улицу; у настольной кассы других способов узнать себя нет.
 * Зато адрес торговой точки известен: он выбран в государственном
 * регистре, и по нему поиск находит дом.
 *
 * Поиск ограничен Казахстаном: точка стоит здесь, а одноимённых улиц
 * в мире достаточно, чтобы увести карту на другой континент.
 *
 * **Про службу.** Поиск открытый, отдан сообществом OpenStreetMap,
 * и его правила требуют называть себя и не заваливать запросами.
 * Обращение идёт по нажатию владельца, а не за набором, — и адрес
 * ищут раз в жизни точки. Перед выпуском на всех владельцев служба
 * заменяется на свою или оплаченную: адрес вынесен в [service].
 */
class MapGeocoder(private val service: String = NOMINATIM) {

    /** Что нашлось по адресу. Пусто — не нашлось или сети нет. */
    suspend fun find(address: String): List<MapPlace> = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext emptyList()
        runCatching {
            val query = URLEncoder.encode(address.trim(), Charsets.UTF_8)
            val url = "$service?format=jsonv2&limit=$LIMIT&countrycodes=kz&accept-language=ru&q=$query"
            val connection = URI.create(url).toURL().openConnection()
            connection.setRequestProperty("User-Agent", AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val text = connection.getInputStream().use { it.reader().readText() }
            json.decodeFromString<List<FoundPlace>>(text).mapNotNull(FoundPlace::place)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val NOMINATIM = "https://nominatim.openstreetmap.org/search"
        const val AGENT = "Superkassa/1.0 (kassa workplace)"
        const val TIMEOUT_MS = 8_000
        const val LIMIT = 5

        val json = Json { ignoreUnknownKeys = true }
    }
}

/** Найденное службой; координаты приходят строками, а имя поля — с подчёркиванием. */
@Serializable
private data class FoundPlace(
    val lat: String? = null,
    val lon: String? = null,
    @SerialName("display_name") val displayName: String? = null
) {

    fun place(): MapPlace? {
        val latitude = lat?.toDoubleOrNull() ?: return null
        val longitude = lon?.toDoubleOrNull() ?: return null
        return MapPlace(latitude, longitude, displayName.orEmpty())
    }
}
