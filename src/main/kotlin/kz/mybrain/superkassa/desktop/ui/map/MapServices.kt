package kz.mybrain.superkassa.desktop.ui.map

import kz.mybrain.superkassa.desktop.app.Preferences

/**
 * Службы карты, собранные по настройкам рабочего места.
 *
 * Плитки, поиск адреса, место по точке и определение своего места —
 * чужие службы, и каждая задаётся своим адресом. Пока адрес не задан,
 * работают общедоступные службы сообщества: их правила запрещают массовую
 * выкачку, и на выпуск для всех владельцев они не рассчитаны. Замена — настройка, а не правка
 * кода: адреса лежат в настройках рабочего места.
 */
class MapServices(preferences: Preferences) {

    val tiles = MapTiles(preferences.maps.tiles.orEmpty().ifBlank { MapService.TILES })

    val geocoder = MapGeocoder(preferences.maps.search.orEmpty().ifBlank { MapService.SEARCH })

    val reverse = MapReverseGeocoder(preferences.maps.reverse.orEmpty().ifBlank { MapService.REVERSE })

    val locator = MapLocator(preferences.maps.location.orEmpty().ifBlank { MapService.LOCATION })
}

/**
 * Общедоступные службы сообщества.
 *
 * Годятся для стенда и десятка касс. Перед выпуском на всех владельцев
 * заменяются своими или оплаченными — каждая своей настройкой.
 */
object MapService {

    /** Плитки карты OpenStreetMap. */
    const val TILES = "https://tile.openstreetmap.org"

    /** Поиск адреса Nominatim. */
    const val SEARCH = "https://nominatim.openstreetmap.org/search"

    /** Место по точке на карте — обратное геокодирование Nominatim. */
    const val REVERSE = "https://nominatim.openstreetmap.org/reverse"

    /** Город по адресу подключения. */
    const val LOCATION = "https://ipinfo.io/json"

    /** Чем приложение называет себя чужой службе. */
    const val AGENT = "Superkassa/1.0 (kassa workplace)"
}
