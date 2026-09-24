package kz.mybrain.superkassa.integrations.maps

/**
 * Найденное место: координаты и то, как его назвала служба, — чтобы
 * владелец видел, куда его привели.
 *
 * @property latitude широта в градусах.
 * @property longitude долгота в градусах.
 * @property title имя места словами службы: адрес целиком или город.
 */
data class MapPlace(val latitude: Double, val longitude: Double, val title: String)

/**
 * Что за место под точкой, словами картографической службы.
 *
 * Названия здесь чужие: их дала карта, а не государственный регистр.
 * Адресом торговой точки ни одно из них не становится — по ним только
 * отыскивается запись регистра.
 *
 * @property region область; у городов республиканского значения — сам город.
 * @property localities пункты от крупного к мелкому: город, потом район.
 *   Регистр держит улицы то под городом, то под районом, и знать заранее,
 *   какой из них нужен, нельзя.
 * @property street улица; пусто — служба её не назвала.
 * @property house номер дома; пусто — служба его не назвала.
 */
data class MapPointPlace(
    val region: String,
    val localities: List<String>,
    val street: String,
    val house: String
)

/**
 * Плитка карты: масштаб и номер по схеме «slippy map» OSM.
 *
 * @property zoom масштаб, 0 — весь мир одной плиткой.
 * @property x номер столбца.
 * @property y номер строки.
 */
data class MapTile(val zoom: Int, val x: Int, val y: Int)
