package kz.mybrain.superkassa.integrations.maps

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Адреса служб карты и то, как с ними обходиться.
 *
 * По умолчанию — общедоступные службы сообщества OpenStreetMap. Они годятся
 * для стенда и десятка касс; перед выпуском на всех владельцев их заменяют
 * своими или оплаченными, каждую своей настройкой — перевыпуск ради этого
 * не нужен.
 *
 * @property tiles основание плиток: к нему дописывается `/{z}/{x}/{y}.png`.
 * @property search поиск адреса (Nominatim `search`).
 * @property reverse место по точке (Nominatim `reverse`).
 * @property location город по адресу подключения (ответ в форме ipinfo.io).
 * @property userAgent чем приложение представляется службе: правила OSM
 *   запрещают безымянные запросы и запросы с чужим именем.
 * @property searchPause пауза между вопросами к поиску и к месту по точке:
 *   правила Nominatim — не чаще раза в секунду на всё приложение.
 * @property tileWait сколько ждать плитку.
 * @property searchWait сколько ждать поиск и место по точке.
 * @property locationWait сколько ждать город по адресу подключения.
 */
data class MapServices(
    val tiles: String = TILES,
    val search: String = SEARCH,
    val reverse: String = REVERSE,
    val location: String = LOCATION,
    val userAgent: String = USER_AGENT,
    val searchPause: Duration = SEARCH_PAUSE,
    val tileWait: Duration = TILE_WAIT,
    val searchWait: Duration = SEARCH_WAIT,
    val locationWait: Duration = LOCATION_WAIT
) {
    /** Адреса служб сообщества и правила обращения по умолчанию. */
    companion object {
        /** Плитки карты OpenStreetMap. */
        const val TILES: String = "https://tile.openstreetmap.org"

        /** Поиск адреса Nominatim. */
        const val SEARCH: String = "https://nominatim.openstreetmap.org/search"

        /** Место по точке — обратное геокодирование Nominatim. */
        const val REVERSE: String = "https://nominatim.openstreetmap.org/reverse"

        /** Город по адресу подключения. */
        const val LOCATION: String = "https://ipinfo.io/json"

        /** Имя приложения для служб карты. */
        const val USER_AGENT: String = "Superkassa/1.0 (kassa workplace)"

        /** С запасом к «раз в секунду»: часы службы и рабочего места расходятся. */
        val SEARCH_PAUSE: Duration = 1_100.milliseconds

        /** Плитка — картинка в десятки килобайт; дольше пяти секунд её не ждут. */
        val TILE_WAIT: Duration = 5.seconds

        /** Поиск по адресу у общедоступной службы бывает медленным. */
        val SEARCH_WAIT: Duration = 8.seconds

        /** Город по адресу подключения — один короткий ответ. */
        val LOCATION_WAIT: Duration = 6.seconds
    }
}
