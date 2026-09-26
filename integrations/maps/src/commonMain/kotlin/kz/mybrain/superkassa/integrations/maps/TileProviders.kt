package kz.mybrain.superkassa.integrations.maps

/**
 * Каталог поставщиков плиток. Новый поставщик — одна запись здесь.
 *
 * Все поставщики — без ключей доступа: владелец выбирает карту, которую
 * привык видеть, и ничего не регистрирует. По умолчанию — OpenStreetMap:
 * его данные открыты (ODbL), и показывать их можно в любом приложении
 * с подписью авторства.
 *
 * **Условия 2ГИС, Яндекса и Google** разрешают показ их плиток только
 * в их собственных библиотеках и по ключу; прямое обращение к серверам
 * плиток ими не предусмотрено и может быть в любой момент закрыто.
 * Поставщики добавлены по решению владельца, риск — его; для выпуска на всех
 * владельцев — OpenStreetMap или свой сервер плиток (поле «Плитки карты»
 * в настройках, адрес в формате OSM).
 *
 * Поиск адреса и место по метке для всех поставщиков — Nominatim
 * (OpenStreetMap): он тоже без ключа.
 */
object TileProviders {

    /** OpenStreetMap — по умолчанию. */
    val OpenStreetMap: TileProvider = TileProvider(
        id = "osm",
        name = "OpenStreetMap",
        template = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        attribution = "© OpenStreetMap"
    )

    /** 2ГИС: подробная карта городов Казахстана. */
    val TwoGis: TileProvider = TileProvider(
        id = "2gis",
        name = "2ГИС",
        template = "https://tile{s}.maps.2gis.com/tiles?x={x}&y={y}&z={z}&v=1",
        subdomains = listOf("0", "1", "2", "3"),
        attribution = "© 2ГИС",
        maxZoom = 18
    )

    /** Яндекс: сетка эллиптическая — см. [TileGrid.EllipticalMercator]. */
    val Yandex: TileProvider = TileProvider(
        id = "yandex",
        name = "Яндекс",
        template = "https://core-renderer-tiles.maps.yandex.net/tiles?l=map&x={x}&y={y}&z={z}&scale=1&lang={locale}",
        grid = TileGrid.EllipticalMercator,
        attribution = "© Яндекс"
    )

    /** Google. */
    val Google: TileProvider = TileProvider(
        id = "google",
        name = "Google",
        template = "https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}&hl={lang}",
        subdomains = listOf("0", "1", "2", "3"),
        attribution = "© Google",
        maxZoom = 20
    )

    /** Все поставщики в порядке выбора; первый — по умолчанию. */
    val all: List<TileProvider> = listOf(OpenStreetMap, TwoGis, Yandex, Google)

    /** Поставщик по имени; незнакомое или пустое имя — [OpenStreetMap]. */
    fun byId(id: String?): TileProvider = all.firstOrNull { it.id == id } ?: OpenStreetMap

    /**
     * Свой сервер плиток в формате OSM (`{основание}/{z}/{x}/{y}.png`) —
     * им заменяют общедоступные службы перед выпуском на всех владельцев.
     */
    fun custom(base: String): TileProvider = OpenStreetMap.copy(
        id = CUSTOM,
        template = base.trimEnd('/') + "/{z}/{x}/{y}.png"
    )

    /** Имя своего сервера плиток в каталоге плиток на диске. */
    const val CUSTOM: String = "custom"
}
