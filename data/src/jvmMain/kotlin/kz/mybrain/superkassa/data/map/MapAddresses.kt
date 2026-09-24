package kz.mybrain.superkassa.data.map

import kz.mybrain.superkassa.data.local.MapPreferences
import kz.mybrain.superkassa.integrations.maps.MapServices

/**
 * Адреса служб карты по настройкам рабочего места.
 *
 * Читаются при каждом обращении, а не один раз: адрес меняют в настройках,
 * и перезапуск ради этого не нужен. Пустая настройка — общедоступная
 * служба сообщества, адрес по умолчанию модуля карт.
 */
class MapAddresses(private val preferences: MapPreferences) {

    /** Службы карты сейчас: заданные владельцем, а не заданные — общедоступные. */
    fun services(): MapServices = MapServices(
        tiles = preferences.tiles.orEmpty().ifBlank { MapServices.TILES },
        search = preferences.search.orEmpty().ifBlank { MapServices.SEARCH },
        reverse = preferences.reverse.orEmpty().ifBlank { MapServices.REVERSE },
        location = preferences.location.orEmpty().ifBlank { MapServices.LOCATION }
    )
}
