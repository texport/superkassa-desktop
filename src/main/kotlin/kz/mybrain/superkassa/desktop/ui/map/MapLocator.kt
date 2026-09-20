package kz.mybrain.superkassa.desktop.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

/**
 * Приблизительное место рабочего места.
 *
 * У настольной кассы нет ни спутникового приёмника, ни разрешений
 * телефона: единственное, чем машина может себя выдать, — адрес, с
 * которого она выходит в сеть. Внешняя служба сопоставляет его с городом
 * и отдаёт его середину.
 *
 * Отсюда и точность: **город, а не дом**. Место годится, чтобы карта
 * открылась в нужном городе, а не посреди страны; саму точку владелец
 * ставит нажатием.
 *
 * Обращение делается только по разрешению владельца: адрес подключения
 * уходит наружу, и решение об этом принимает он, а не приложение.
 * Ни имени, ни реквизитов кассы в запросе нет — служба видит только тот
 * адрес, с которого пришёл запрос.
 */
class MapLocator(val service: String = MapService.LOCATION) {

    /** Где мы, по мнению службы. `null` — сети нет или служба не ответила. */
    suspend fun locate(): MapPlace? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URI.create(service).toURL().openConnection()
            connection.setRequestProperty("User-Agent", MapService.AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val text = connection.getInputStream().use { it.reader().readText() }
            placeOf(json.decodeFromString<LocationAnswer>(text))
        }.getOrNull()
    }

    /** Разбирает ответ: `loc` приходит парой «широта,долгота» одной строкой. */
    private fun placeOf(answer: LocationAnswer): MapPlace? {
        val parts = answer.loc?.split(',') ?: return null
        if (parts.size != 2) return null
        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        return MapPlace(latitude, longitude, answer.city.orEmpty())
    }

    private companion object {
        const val TIMEOUT_MS = 6_000

        val json = Json { ignoreUnknownKeys = true }
    }
}

/** Найденное место: координаты и город, чтобы владелец видел, куда его привели. */
data class MapPlace(val latitude: Double, val longitude: Double, val city: String)

/** Ответ службы; остальные её поля приложению не нужны и не читаются. */
@Serializable
private data class LocationAnswer(val loc: String? = null, val city: String? = null)
