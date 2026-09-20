package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.components.narrowed

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
 * Точки и кассы раскрытой точки одним списком, суженные поиском.
 *
 * Точка остаётся в списке, когда подходит сама или когда подходит хоть
 * одна её касса: владелец ищет кассу по номеру КГД и не обязан помнить,
 * в какой она точке. Подошедшая по имени точка показывает все свои кассы,
 * а найденная по кассе — только найденные.
 *
 * @param open раскрытая точка; кассы показываются только у неё.
 */
fun placeRows(
    places: List<RetailPlace>,
    registers: List<CabinetRegister>,
    open: String?,
    query: String = ""
): List<PlaceRow> {
    val needle = query.trim()
    val byPlace = registers.groupBy { it.retailPlace?.id }
    return places.flatMap { place ->
        val own = byPlace[place.id].orEmpty()
        val placeFound = narrowed(listOf(place), needle, ::placeKeys).isNotEmpty()
        val found = if (placeFound) own else narrowed(own, needle, ::registerKeys)
        when {
            !placeFound && found.isEmpty() -> emptyList()
            place.id != open -> listOf(PlaceRow.Point(place))
            else -> listOf(PlaceRow.Point(place)) + found.map(PlaceRow::Register)
        }
    }
}

/** По чему находится торговая точка: по названию и по адресу. */
private fun placeKeys(place: RetailPlace): List<String> =
    listOfNotNull(place.name, place.address, place.addressKz)

/** По чему находится касса: по своему названию, номеру КГД и заводскому. */
private fun registerKeys(register: CabinetRegister): List<String> =
    listOfNotNull(register.internalName, register.registrationNumber, register.factoryNumber)
