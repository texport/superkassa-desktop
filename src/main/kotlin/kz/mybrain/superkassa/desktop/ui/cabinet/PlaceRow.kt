package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.components.narrowed
import kz.mybrain.superkassa.desktop.ui.strings.Language

/**
 * Строка колонки торговых точек: сама точка или касса под ней.
 *
 * Дерево отдаётся плоским списком, а не вложенными столбцами: у владельца
 * бывают сотни точек, и рисовать их можно только построчно — списком,
 * который держит на экране лишь видимые строки. Вложенность остаётся
 * в самом типе строки, и по нему разметка знает, что рисовать.
 */
sealed interface PlaceRow {

    /** Ключ строки для списка: по нему список узнаёт строку между перерисовками. */
    val id: String

    data class Point(val place: RetailPlace) : PlaceRow {
        override val id: String get() = place.id
    }

    data class Register(val register: CabinetRegister) : PlaceRow {
        override val id: String get() = register.id
    }
}

/**
 * Точки и кассы раскрытой точки одним списком — отобранные и выстроенные.
 *
 * Точка остаётся в списке, когда подходит сама или когда подходит хоть
 * одна её касса: владелец ищет кассу по номеру КГД и не обязан помнить,
 * в какой она точке. Подошедшая по имени точка показывает все свои кассы,
 * а найденная по кассе — только найденные.
 *
 * Когда отбор спрашивает о самой кассе — о состоянии учёта или о
 * блокировке, — подошедшего имени точке мало: на вопрос «где отказ КГД»
 * точка без отказов не отвечает, как бы она ни называлась.
 *
 * @param open раскрытая точка; кассы показываются только у неё.
 * @param locked кассы, заблокированные по словам кабинета.
 * @param language на каком языке брать адрес при порядке по адресу:
 *   регистр отдаёт его и по-русски, и по-казахски.
 */
fun placeRows(
    places: List<RetailPlace>,
    registers: List<CabinetRegister>,
    open: String?,
    sieve: PlaceSieve = PlaceSieve(),
    locked: Set<String> = emptySet(),
    language: Language = Language.Ru
): List<PlaceRow> {
    val byPlace = registers.groupBy { it.retailPlace?.id }
    val kept = places.mapNotNull { place -> sieved(place, byPlace[place.id].orEmpty(), sieve, locked) }
    return sortedPlaces(kept, sieve, language) { attentionOf(byPlace[it.id].orEmpty()) }
        .flatMap { row -> rowsOf(row, open) }
}

/** Точка с её кассами после отбора; `null` — из списка она ушла. */
private fun sieved(
    place: RetailPlace,
    own: List<CabinetRegister>,
    sieve: PlaceSieve,
    locked: Set<String>
): SievedPlace? {
    val marked = own.filter { sieve.keeps(it, locked) }
    val placeFound = narrowed(listOf(place), sieve.needle, ::placeKeys).isNotEmpty()
    val found = if (placeFound) marked else narrowed(marked, sieve.needle, ::registerKeys)
    val keeps = if (sieve.marked) found.isNotEmpty() else placeFound || found.isNotEmpty()
    return if (keeps) SievedPlace(place, found) else null
}

/** Строки одной точки: сама точка, а у раскрытой — и её кассы. */
private fun rowsOf(row: SievedPlace, open: String?): List<PlaceRow> = when (row.place.id) {
    open -> listOf(PlaceRow.Point(row.place)) + row.registers.map(PlaceRow::Register)
    else -> listOf(PlaceRow.Point(row.place))
}

/** По чему находится торговая точка: по названию и по адресу. */
private fun placeKeys(place: RetailPlace): List<String> =
    listOfNotNull(place.name, place.address, place.addressKz)

/** По чему находится касса: по своему названию, номеру КГД и заводскому. */
private fun registerKeys(register: CabinetRegister): List<String> =
    listOfNotNull(register.internalName, register.registrationNumber, register.factoryNumber)
