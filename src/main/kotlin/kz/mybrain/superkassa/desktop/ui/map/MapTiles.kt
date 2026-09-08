package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Плитки карты: где взять и где держать.
 *
 * Плитки берутся из открытой карты OpenStreetMap и складываются на диск
 * рядом с остальными настройками рабочего места. Раз положенная плитка
 * больше не запрашивается: карта нужна на минуту при заведении точки,
 * и качать её заново при каждом открытии — впустую занимать и сеть,
 * и чужую службу.
 *
 * **Про службу плиток.** Открытые плитки OpenStreetMap отданы сообществом
 * и их правила запрещают массовую выкачку. Для стенда и десятка касс
 * этого достаточно; перед выпуском на всех владельцев источник плиток
 * заменяется на свой или оплаченный — адрес для этого вынесен в [source].
 */
class MapTiles(
    private val source: String = OPEN_STREET_MAP,
    private val folder: File = File(System.getProperty("user.home"), ".superkassa/tiles")
) {

    private val loaded = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    /** Плитка, если она уже под рукой. Показ рисует только то, что есть. */
    fun ready(zoom: Int, x: Int, y: Int): ImageBitmap? = loaded[key(zoom, x, y)]

    /** Пробовали ли уже взять эту плитку и не смогли. */
    fun failed(zoom: Int, x: Int, y: Int): Boolean = key(zoom, x, y) in missing

    /**
     * Достаёт плитку: сначала с диска, потом из сети.
     *
     * Возвращает `false`, если плитки нет и взять её неоткуда, — карта
     * в этом случае остаётся сеткой, а выбор точки продолжает работать:
     * координаты считаются из проекции, а не из картинки.
     */
    suspend fun fetch(zoom: Int, x: Int, y: Int): Boolean {
        val key = key(zoom, x, y)
        if (loaded.containsKey(key) || key in missing) return loaded.containsKey(key)
        return withContext(Dispatchers.IO) {
            val bytes = fromDisk(zoom, x, y) ?: fromNetwork(zoom, x, y)
            val bitmap = bytes?.let { decoded(it) }
            if (bitmap == null) {
                missing += key
                false
            } else {
                loaded[key] = bitmap
                true
            }
        }
    }

    private fun fromDisk(zoom: Int, x: Int, y: Int): ByteArray? =
        file(zoom, x, y).takeIf { it.isFile && it.length() > 0 }?.readBytes()

    private fun fromNetwork(zoom: Int, x: Int, y: Int): ByteArray? = runCatching {
        val connection = URI.create("$source/$zoom/$x/$y.png").toURL().openConnection()
        connection.setRequestProperty("User-Agent", AGENT)
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        val bytes = connection.getInputStream().use { it.readBytes() }
        file(zoom, x, y).apply { parentFile?.mkdirs() }.writeBytes(bytes)
        bytes
    }.getOrNull()

    private fun decoded(bytes: ByteArray): ImageBitmap? =
        runCatching { bytes.inputStream().use(::loadImageBitmap) }.getOrNull()

    private fun file(zoom: Int, x: Int, y: Int): File = File(folder, "$zoom/$x/$y.png")

    private fun key(zoom: Int, x: Int, y: Int): String = "$zoom/$x/$y"

    private companion object {
        const val OPEN_STREET_MAP = "https://tile.openstreetmap.org"
        const val AGENT = "Superkassa/1.0 (kassa workplace)"
        const val TIMEOUT_MS = 5_000
    }
}
