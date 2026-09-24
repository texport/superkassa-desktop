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
 * @param folder путь к каталогу плиток: у каждой платформы свой каталог данных.
 */
class DiskTiles(folder: String) : TileStore {
    private val folder = Path(folder)

    override suspend fun read(tile: MapTile): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileOf(tile)
            val size = SystemFileSystem.metadataOrNull(file)?.takeIf { it.isRegularFile }?.size ?: 0L
            if (size > 0) SystemFileSystem.source(file).buffered().use { it.readByteArray() } else null
        }.getOrNull()
    }

    override suspend fun write(tile: MapTile, image: ByteArray) = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileOf(tile)
            file.parent?.let { SystemFileSystem.createDirectories(it) }
            SystemFileSystem.sink(file).buffered().use { it.write(image) }
        }
        Unit
    }

    private fun fileOf(tile: MapTile): Path = Path(folder, "${tile.zoom}", "${tile.x}", "${tile.y}.png")
}
