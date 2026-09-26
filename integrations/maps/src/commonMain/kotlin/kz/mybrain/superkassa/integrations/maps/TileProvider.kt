package kz.mybrain.superkassa.integrations.maps

/**
 * Сетка плиток поставщика: как широта ложится на строки плиток.
 *
 * Долгота у всех одна, а широта — нет: OpenStreetMap, 2ГИС и Google режут
 * мир сферической проекцией Меркатора (EPSG:3857), Яндекс — эллиптической
 * (EPSG:3395). Плитка Яндекса, положенная в сферическую сетку как есть,
 * уезжает на юг на сотни метров.
 */
enum class TileGrid {
    /** Сферический Меркатор, EPSG:3857. */
    WebMercator,

    /** Эллиптический Меркатор, EPSG:3395. */
    EllipticalMercator
}

/**
 * Поставщик плиток карты — одна запись каталога [TileProviders].
 *
 * @property id имя поставщика в настройках и в каталоге плиток на диске:
 *   плитки разных поставщиков не смешиваются.
 * @property name название поставщика — его собственное имя, одно на всех языках.
 * @property template адрес плитки с подстановками `{z}`, `{x}`, `{y}`,
 *   `{s}` — поддомен, `{lang}` — язык (`ru`), `{locale}` — язык с регионом (`ru_RU`).
 * @property subdomains поддомены на место `{s}`; плитки делятся между ними,
 *   чтобы не упираться в предел соединений одного адреса.
 * @property grid сетка плиток.
 * @property attribution подпись авторства: её требуют условия поставщика,
 *   и карта показывает её в углу.
 * @property maxZoom самое крупное увеличение, которое поставщик отдаёт.
 */
data class TileProvider(
    val id: String,
    val name: String,
    val template: String,
    val subdomains: List<String> = emptyList(),
    val grid: TileGrid = TileGrid.WebMercator,
    val attribution: String,
    val maxZoom: Int = MAX_ZOOM
) {
    /** Адрес плитки [tile] на языке [language] (`ru`, `kk`, `en`). */
    fun url(tile: MapTile, language: String): String {
        val subdomain = subdomains.takeIf { it.isNotEmpty() }?.let { it[(tile.x + tile.y).mod(it.size)] }.orEmpty()
        return template
            .replace("{z}", tile.zoom.toString())
            .replace("{x}", tile.x.toString())
            .replace("{y}", tile.y.toString())
            .replace("{s}", subdomain)
            .replace("{lang}", language)
            .replace("{locale}", LOCALES[language] ?: LOCALES.getValue(DEFAULT_LANGUAGE))
    }

    private companion object {
        const val MAX_ZOOM = 19
        const val DEFAULT_LANGUAGE = "ru"
        val LOCALES = mapOf("ru" to "ru_RU", "kk" to "kk_KZ", "en" to "en_US")
    }
}
