package kz.mybrain.superkassa.domain.workplace.model

/**
 * Чьими службами рисуется карта.
 *
 * @property tiles плитки карты.
 * @property search поиск адреса торговой точки.
 * @property reverse место под поставленной на карте меткой.
 * @property location своё место по адресу подключения.
 */
data class MapServices(
    val tiles: String? = null,
    val search: String? = null,
    val reverse: String? = null,
    val location: String? = null
)

/** Адреса без пробелов по краям; пустой адрес — общедоступная служба. */
fun MapServices.tidy(): MapServices = MapServices(tiles.tidy(), search.tidy(), reverse.tidy(), location.tidy())

/** Хоть одна служба своя, а не общедоступная. */
val MapServices.custom: Boolean get() = listOf(tiles, search, reverse, location).any { !it.isNullOrBlank() }

private fun String?.tidy(): String? = this?.trim()?.ifBlank { null }
