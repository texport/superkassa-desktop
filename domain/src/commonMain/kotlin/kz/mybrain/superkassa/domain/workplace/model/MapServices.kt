package kz.mybrain.superkassa.domain.workplace.model

/**
 * Чьими службами рисуется карта.
 *
 * @property tiles плитки карты.
 * @property search поиск адреса торговой точки.
 * @property reverse место под поставленной на карте меткой.
 * @property location своё место по адресу подключения.
 * @property provider поставщик плиток ([kz.mybrain.superkassa.domain.map.model.MapProvider.id]);
 *   `null` — по умолчанию. Свой сервер плиток [tiles] важнее поставщика.
 */
data class MapServices(
    val tiles: String? = null,
    val search: String? = null,
    val reverse: String? = null,
    val location: String? = null,
    val provider: String? = null
)

/** Адреса без пробелов по краям; пустой адрес — общедоступная служба. */
fun MapServices.tidy(): MapServices =
    MapServices(tiles.tidy(), search.tidy(), reverse.tidy(), location.tidy(), provider.tidy())

/** Хоть одна служба своя, а не общедоступная, или выбран не поставщик по умолчанию. */
val MapServices.custom: Boolean get() = listOf(tiles, search, reverse, location, provider).any { !it.isNullOrBlank() }

private fun String?.tidy(): String? = this?.trim()?.ifBlank { null }
