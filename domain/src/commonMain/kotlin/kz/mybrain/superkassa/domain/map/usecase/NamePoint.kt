package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.MapPointPlace
import kz.mybrain.superkassa.domain.map.port.Maps

/** Что за место под меткой, словами службы карт: по нему ищется запись регистра. */
class NamePoint(private val maps: Maps) {

    /** @return место; `null` — не узнано или служба не ответила. */
    suspend operator fun invoke(latitude: Double, longitude: Double): MapPointPlace? =
        maps.placeAt(latitude, longitude)
}
