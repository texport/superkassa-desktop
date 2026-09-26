package kz.mybrain.superkassa.domain.map.model

/**
 * Сетка плиток поставщика: как широта ложится на строки плиток.
 *
 * Долгота у всех одна, а широта — нет: большинство поставщиков режут мир
 * сферической проекцией Меркатора (EPSG:3857), Яндекс — эллиптической
 * (EPSG:3395). Карта ставит плитку по сетке её поставщика, а метки —
 * всегда в сферической.
 */
enum class TileGrid {
    /** Сферический Меркатор, EPSG:3857. */
    WebMercator,

    /** Эллиптический Меркатор, EPSG:3395. */
    EllipticalMercator
}

/**
 * Чьи плитки у карты: владелец выбирает карту, которую привык видеть.
 *
 * @property id имя поставщика в настройках рабочего места.
 * @property name собственное имя поставщика — одно на всех языках.
 * @property attribution подпись авторства в углу карты: её требуют условия поставщика.
 * @property grid сетка плиток.
 * @property maxZoom самое крупное увеличение, которое поставщик отдаёт.
 */
data class MapProvider(
    val id: String,
    val name: String,
    val attribution: String,
    val grid: TileGrid = TileGrid.WebMercator,
    val maxZoom: Int = OSM_MAX_ZOOM
) {
    /** Поставщик по умолчанию. */
    companion object {
        private const val OSM_MAX_ZOOM = 19

        /** OpenStreetMap — карта, которой рабочее место пользуется, пока владелец не выбрал другую. */
        val OpenStreetMap: MapProvider =
            MapProvider(id = "osm", name = "OpenStreetMap", attribution = "© OpenStreetMap")
    }
}
