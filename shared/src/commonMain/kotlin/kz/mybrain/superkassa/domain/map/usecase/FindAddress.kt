package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.port.Maps

/** Места по адресу словами владельца, ближайшее к запросу первым. */
class FindAddress(private val maps: Maps) {

    /** @return места; пусто — не нашлось; `null` — служба не ответила. */
    suspend operator fun invoke(address: String): List<MapPlace>? = maps.find(address)
}
