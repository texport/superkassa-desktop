package kz.mybrain.superkassa.domain.analytics.model

/**
 * Свод торговых точек по регионам.
 *
 * Отделён от прочего счёта итогов: сложение по регионам опирается
 * на справочник точек, а не на числа срока, и проверяется оно своими
 * составами — адресом с регионом, точкой без адреса и точкой, которой
 * в справочнике нет вовсе.
 */

/** Торговая точка для свода по регионам: регион стоит в её адресе. */
data class PlaceAddress(val id: String, val name: String, val address: String?)

/** Регион сети: сколько в нём точек, чем торговал и какая доля сети на него пришлась. */
data class SalesRegion(
    val title: String,
    val placeCount: Int,
    val registerCount: Int,
    val receiptCount: Int,
    val revenue: Long,
    val percent: Int
)

/**
 * Свод торговых точек по регионам.
 *
 * Регион берётся из адреса точки — первым звеном до запятой: адрес
 * кабинета записан как «Регион, Район, Улица, Дом», и своего поля
 * региона у точки нет. Точка без адреса не выбрасывается из свода:
 * её выручка входит в сеть, и потерять её значит показать министру
 * неполную сумму.
 *
 * Касс с чеками считается по кассам, а не по точкам: точка сравнивается
 * с точкой выручкой, а число работавших машин — это про сеть.
 */
fun regionsOf(
    places: List<SalesUnit>,
    registers: List<SalesUnit>,
    catalogue: List<PlaceAddress>,
    unknown: String
): List<SalesRegion> {
    val byId = catalogue.associateBy { it.id }
    val byName = catalogue.associateBy { it.name }
    val total = places.fold(0L) { sum, place -> sum + place.revenue.orZero() }
    val selling = registers
        .filter { it.receiptCount > 0 }
        .groupingBy { regionTitle(it.retailPlaceName?.let(byName::get), unknown) }
        .eachCount()
    return places
        .groupBy { regionTitle(place(it, byId, byName), unknown) }
        .map { (title, rows) -> region(title, rows, selling[title] ?: 0, total) }
        .sortedByDescending { it.revenue }
}

/**
 * Какой точке справочника отвечает строка свода.
 *
 * Сперва по ключу, потом по названию: ключ точки кабинет присылает
 * не во всех разрезах, а названия точек в компании уникальны — их
 * же владелец и читает на экране.
 */
private fun place(
    row: SalesUnit,
    byId: Map<String, PlaceAddress>,
    byName: Map<String, PlaceAddress>
): PlaceAddress? = row.id?.let(byId::get) ?: row.name?.let(byName::get)

/** Название региона точки; сам разбор адреса — общий на раздел, см. [regionOf]. */
private fun regionTitle(place: PlaceAddress?, unknown: String): String =
    regionOf(place?.address, unknown)

/** Одна строка свода: точки региона, сложенные вместе. */
private fun region(title: String, rows: List<SalesUnit>, registers: Int, total: Long): SalesRegion {
    val revenue = rows.fold(0L) { sum, row -> sum + row.revenue.orZero() }
    return SalesRegion(
        title = title,
        placeCount = rows.size,
        registerCount = registers,
        receiptCount = rows.sumOf { it.receiptCount },
        revenue = revenue,
        percent = if (total <= 0) 0 else percentOf(revenue, total)
    )
}
