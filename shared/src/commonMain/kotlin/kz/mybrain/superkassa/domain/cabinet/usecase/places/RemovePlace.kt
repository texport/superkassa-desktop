package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces

/** Удаляет торговую точку: кабинет удаляет только точку без касс. */
class RemovePlace(private val places: CabinetPlaces) {
    suspend operator fun invoke(place: RetailPlace) = places.remove(place.id)
}
