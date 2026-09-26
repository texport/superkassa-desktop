package kz.mybrain.superkassa.data.map

import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.model.MapPointPlace
import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.integrations.maps.MapTile
import kz.mybrain.superkassa.integrations.maps.OpenMaps
import kz.mybrain.superkassa.integrations.maps.MapPlace as ServicePlace
import kz.mybrain.superkassa.integrations.maps.MapPointPlace as ServicePointPlace

/**
 * Службы карты через модуль карт: плитки, поиск адреса и место под точкой.
 *
 * Модуль знает протокол служб и их правила; здесь — только перевод в порт
 * и то, чего модуль не знает: язык владельца и своё место от самой машины.
 *
 * Осечку обращения модуль сам называет молчанием службы (`null`) — и сбой
 * связи, и адрес, по которому сеть не ходит вовсе.
 *
 * @param maps службы карт модуля.
 * @param language язык названий в ответах служб — язык владельца (`ru`, `kk`, `en`).
 * @param machine своё место от службы геопозиции машины; на платформе без
 *   такой службы — `null`.
 */
class OpenStreetMaps(
    private val maps: OpenMaps,
    private val language: () -> String,
    private val machine: suspend () -> MapPlace? = { null }
) : Maps {

    override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = maps.tile(MapTile(zoom, x, y), language())

    override fun provider(): MapProvider = maps.provider().domain()

    override suspend fun find(address: String): List<MapPlace>? =
        maps.find(address, language())?.map { it.domain() }

    override suspend fun placeAt(latitude: Double, longitude: Double): MapPointPlace? =
        maps.placeAt(latitude, longitude, language())?.domain()

    override suspend fun locateMachine(): MapPlace? = machine()

    override suspend fun locateByConnection(): MapPlace? = maps.locateByConnection()?.domain()
}

private fun ServicePlace.domain(): MapPlace = MapPlace(latitude, longitude, title)

private fun ServicePointPlace.domain(): MapPointPlace = MapPointPlace(region, localities, street, house)
