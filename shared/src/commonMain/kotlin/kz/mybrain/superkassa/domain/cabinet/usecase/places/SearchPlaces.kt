package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces

/**
 * Точки, найденные кабинетом по набранному, — одной страницей.
 *
 * Для выбора точки кассы: кабинет ищет сам, и ради одной точки
 * не читаются все страницы сети.
 */
class SearchPlaces(private val places: CabinetPlaces) {
    suspend operator fun invoke(text: String): List<RetailPlace> = places.search(text)
}
