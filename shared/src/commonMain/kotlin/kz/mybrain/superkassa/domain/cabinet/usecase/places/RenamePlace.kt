package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces

/** Переименовывает торговую точку. */
class RenamePlace(private val places: CabinetPlaces) {
    suspend operator fun invoke(place: RetailPlace, name: String): RetailPlace = places.rename(place.id, name.trim())
}
