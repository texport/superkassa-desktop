package kz.mybrain.superkassa.data.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kz.mybrain.superkassa.integrations.maps.MapTile
import kz.mybrain.superkassa.integrations.maps.TileStore

/**
 * Плитки карты на диске рабочего места.
 *
 * Раз положенная плитка больше не запрашивается: карта нужна на минуту
 * при заведении точки, и качать её заново при каждом открытии — впустую
 * занимать и сеть, и чужую службу, правила которой запрещают массовую
 * выкачку. Неудача записи карту не роняет: плитка просто спросится снова.
 *
 * Плитки каждого поставщика — в своём подкаталоге: сменивший карту
 * владелец не видит поверх неё плиток прежней.
 *
 * @param folder путь к каталогу плиток: у каждой платформы свой каталог данных.
 */
class DiskTiles(folder: String) : TileStore {
    private val folder = Path(folder)

    override suspend fun read(provider: String, tile: MapTile): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileOf(provider, tile)
            val size = SystemFileSystem.metadataOrNull(file)?.takeIf { it.isRegularFile }?.size ?: 0L
            if (size > 0) SystemFileSystem.source(file).buffered().use { it.readByteArray() } else null
        }.getOrNull()
    }

    override suspend fun write(provider: String, tile: MapTile, image: ByteArray) = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileOf(provider, tile)
            file.parent?.let { SystemFileSystem.createDirectories(it) }
            SystemFileSystem.sink(file).buffered().use { it.write(image) }
        }
        Unit
    }

    private fun fileOf(provider: String, tile: MapTile): Path =
        Path(folder, provider, "${tile.zoom}", "${tile.x}", "${tile.y}.png")
}
