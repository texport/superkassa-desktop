package kz.mybrain.superkassa.data.analytics

import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.model.MapPointPlace
import kz.mybrain.superkassa.domain.map.port.Maps

/** Карты на Android: ни плиток, ни поиска — как без сети. */
class MapsNotOnAndroid : Maps {
    override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = null
    override suspend fun find(address: String): List<MapPlace>? = null
    override suspend fun placeAt(latitude: Double, longitude: Double): MapPointPlace? = null
    override suspend fun locateMachine(): MapPlace? = null
    override suspend fun locateByConnection(): MapPlace? = null
}
