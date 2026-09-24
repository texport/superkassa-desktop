package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces

/**
 * Все торговые точки компании, страница за страницей.
 *
 * @param onPart прочитанное после каждой страницы и сколько точек всего.
 */
class ReadPlaces(private val places: CabinetPlaces) {
    suspend operator fun invoke(onPart: (List<RetailPlace>, Long) -> Unit): List<RetailPlace> = places.all(onPart)
}
