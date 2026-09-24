package kz.mybrain.superkassa.integrations.maps

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.parameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kz.mybrain.superkassa.integrations.maps.wire.MapFetch
import kz.mybrain.superkassa.integrations.maps.wire.SearchPace
import kz.mybrain.superkassa.integrations.maps.wire.locationOf
import kz.mybrain.superkassa.integrations.maps.wire.placesOf
import kz.mybrain.superkassa.integrations.maps.wire.pointPlaceOf
import kotlin.time.TimeSource

/**
 * Службы карты сообщества OpenStreetMap — или свои, заданные настройкой.
 *
 * Все они чужие и отвечают не всегда, и ответ это различает: `null` —
 * служба не ответила, пустой список — ответила, но не нашла. Слитые
 * в одно, «нет сети» и «не нашлось» оставляли адрес ненайденным до
 * перезапуска.
 *
 * Найденные адреса хранятся в памяти на время работы: адрес торговой точки
 * не меняется, и спрашивать о нём заново — и медленно, и против правил
 * службы. Хранится только найденное. Поиск ограничен Казахстаном:
 * одноимённых улиц в мире достаточно, чтобы увести карту на другой континент.
 *
 * @param services адреса служб; читаются при каждом обращении, чтобы
 *   сменённая настройка действовала без перезапуска.
 * @param tiles где хранить полученные плитки.
 * @param journal куда писать неудачи обращений.
 * @param engine движок Ktor; по умолчанию — движок платформы.
 * @param clock часы паузы между вопросами к поиску.
 */
class OpenMaps(
    private val services: () -> MapServices = { MapServices() },
    private val tiles: TileStore = TileStore.None,
    journal: MapJournal = MapJournal.Silent,
    engine: HttpClientEngine = platformEngine(),
    clock: TimeSource = TimeSource.Monotonic
) : AutoCloseable {
    private val http = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout)
    }
    private val fetch = MapFetch(http, journal) { services().userAgent }
    private val pace = SearchPace({ services().searchPause }, clock)
    private val foundLock = Mutex()
    private val found = mutableMapOf<String, List<MapPlace>>()

    /**
     * Плитка карты: из хранилища, а нет там — у службы.
     *
     * @return картинка плитки; `null` — взять её неоткуда.
     */
    suspend fun tile(tile: MapTile): ByteArray? = tiles.read(tile) ?: fetchTile(tile)?.also { tiles.write(tile, it) }

    /**
     * Места по адресу, ближайшее к запросу первым.
     *
     * @param address адрес словами, как его пишет владелец.
     * @param language язык названий в ответе — язык владельца (`ru`, `kk`, `en`).
     * @return места; пусто — не нашлось; `null` — служба не ответила.
     */
    suspend fun find(address: String, language: String): List<MapPlace>? {
        val query = address.trim()
        if (query.isBlank()) return emptyList()
        val key = "$language|$query"
        return foundLock.withLock { found[key] }
            ?: search(query, language)?.also { places -> foundLock.withLock { found[key] = places } }
    }

    /**
     * Что за место под точкой, словами службы.
     *
     * @return место; `null` — не узнано или служба не ответила.
     */
    suspend fun placeAt(latitude: Double, longitude: Double, language: String): MapPointPlace? {
        val current = services()
        val answer = pace.next {
            fetch.get("reverse", current.reverse, current.searchWait) {
                parameter("format", "jsonv2")
                parameter("zoom", HOUSE_DETAIL)
                parameter("accept-language", language)
                parameter("lat", latitude)
                parameter("lon", longitude)
            }
        }
        return answer?.let { pointPlaceOf(it.text()) }
    }

    /**
     * Город по адресу подключения.
     *
     * Адрес машины уходит чужой службе, поэтому спрашивать можно только
     * с разрешения владельца — это решает приложение.
     *
     * @return город с координатами; `null` — служба не ответила.
     */
    suspend fun locateByConnection(): MapPlace? {
        val current = services()
        return fetch.get("location", current.location, current.locationWait)?.let { locationOf(it.text()) }
    }

    /** Закрывает соединения со службами. */
    override fun close() = http.close()

    private suspend fun search(address: String, language: String): List<MapPlace>? {
        val current = services()
        val answer = pace.next {
            fetch.get("search", current.search, current.searchWait) {
                parameter("format", "jsonv2")
                parameter("limit", FOUND_MOST)
                parameter("countrycodes", COUNTRY)
                parameter("accept-language", language)
                parameter("q", address)
            }
        }
        return answer?.let { placesOf(it.text()) }
    }

    /** Плитка у службы; ответ не картинкой — не плитка, и хранить его нельзя. */
    private suspend fun fetchTile(tile: MapTile): ByteArray? {
        val current = services()
        val url = "${current.tiles.trimEnd('/')}/${tile.zoom}/${tile.x}/${tile.y}.png"
        val answer = fetch.get("tiles", url, current.tileWait) ?: return null
        return answer.takeIf { it.contentType.startsWith(IMAGE) }?.bytes
    }

    private companion object {
        /** Сколько мест брать по одному адресу. */
        const val FOUND_MOST = 5

        /** Подробность места по точке: до дома. */
        const val HOUSE_DETAIL = 18

        /** Поиск только по Казахстану. */
        const val COUNTRY = "kz"

        /** Вид ответа с картинкой. */
        const val IMAGE = "image/"
    }
}
