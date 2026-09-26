package kz.mybrain.superkassa.domain.map

import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.model.MapPointPlace
import kz.mybrain.superkassa.domain.map.port.MapMemory
import kz.mybrain.superkassa.domain.map.port.Maps

/**
 * Службы карт без сети: ни плиток, ни своего места.
 *
 * Адреса находятся из [found]; не записанного служба не знает — пустой
 * ответ, а не молчание. Молчание заказывается адресом в [silent]. Своё
 * место машины — [machine], город по адресу подключения — [connection].
 */
class QuietMaps(
    private val found: Map<String, MapPlace> = emptyMap(),
    private val silent: Set<String> = emptySet(),
    private val machine: MapPlace? = null,
    private val connection: MapPlace? = null
) : Maps {
    val searched = mutableListOf<String>()

    /** Сколько раз спросили город по адресу подключения: адрес уходит наружу. */
    var connectionAsked = 0
        private set

    override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = null

    override suspend fun find(address: String): List<MapPlace>? {
        searched += address
        return if (address in silent) null else listOfNotNull(found[address])
    }

    override suspend fun placeAt(latitude: Double, longitude: Double): MapPointPlace? = null

    override suspend fun locateMachine(): MapPlace? = machine

    override suspend fun locateByConnection(): MapPlace? = connection.also { connectionAsked++ }
}

/** Память карт без диска: у каждой проверки своя. */
class MemoryMapMemory(
    override var cardCollapsed: Boolean = false,
    override var legendCollapsed: Boolean = false,
    override var tallyCollapsed: Boolean = false,
    override var locationAllowed: Boolean? = null
) : MapMemory
