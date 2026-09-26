package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.designsystem.image.encodedImage
import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.usecase.ReadTile
import kz.mybrain.superkassa.presentation.common.mapview.grid.TileMemory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Плитки карты, готовые к рисованию, — одного поставщика на время одной карты.
 *
 * Где плитку взять и где её хранить, решает сценарий [ReadTile]: диск
 * рабочего места, потом сеть. Разобранные картинки лежат в общей памяти
 * приложения ([TileMemory]): открытая снова карта рисуется сразу, без
 * диска и без разбора. PNG разбирается вне потока интерфейса: на планшете
 * разбор десятков плиток при открытии карты останавливал показ.
 *
 * Пришедшая плитка только перерисовывает полотно ([version] читается
 * при рисовании), а не пересобирает показ: прежде картинки лежали
 * в состоянии, которое читалось и при сборке, и каждая плитка
 * пересобирала карту вместе со всеми ярлычками касс поверх неё.
 *
 * Не пришедшая плитка не остаётся дырой навсегда: её спрашивают снова,
 * но не чаще раза в [RETRY_AFTER]. Прерванная загрузка неудачей не
 * считается: плитка просто спросится, когда снова попадёт в окно.
 *
 * Спрашиваются плитки из показа карты, одним потоком интерфейса: своей
 * защиты от одновременных обращений учёту не нужно.
 *
 * @property provider чьи плитки: по его сетке они ставятся, его подпись стоит в углу.
 */
class MapTiles(private val readTile: ReadTile, val provider: MapProvider = MapProvider.OpenStreetMap) {

    private val loading = mutableSetOf<String>()
    private var shown = false
    private val failedAt = mutableMapOf<String, TimeMark>()

    /** Сколько плиток пришло: читается при рисовании, чтобы пришедшая плитка перерисовала полотно. */
    var version: Int by mutableIntStateOf(0)
        private set

    /** Сколько раз плитка не пришла; меняется — показ пересматривает, что спросить. */
    var failures: Int by mutableIntStateOf(0)
        private set

    /** Ни одна плитка не пришла, а неудачи были: поле карты надо объяснить. */
    var blank: Boolean by mutableStateOf(false)
        private set

    /** Плитка, если она уже под рукой. Показ рисует только то, что есть. */
    fun ready(zoom: Int, x: Int, y: Int): ImageBitmap? = TileMemory[key(zoom, x, y)]?.also { shown = true }

    /**
     * Плитка предыдущего увеличения, накрывающая эту, — подложка, пока эта
     * не пришла: приближенная карта сразу показывает улицы крупнее, а не
     * серое поле. Берётся только из памяти, у службы её не спрашивают.
     */
    fun cover(zoom: Int, x: Int, y: Int): ImageBitmap? =
        if (zoom > 0) TileMemory[key(zoom - 1, x / 2, y / 2)] else null

    /**
     * Берёт плитку в работу: `true` — её нет, она не грузится и не отказала
     * только что. Взятую плитку показ грузит [fetch] и отпускает [release].
     */
    fun claim(zoom: Int, x: Int, y: Int): Boolean {
        val key = key(zoom, x, y)
        val recent = failedAt[key]?.let { it.elapsedNow() < RETRY_AFTER } == true
        if (key in TileMemory || key in loading || recent) return false
        loading += key
        return true
    }

    /**
     * Достаёт плитку у службы карт и разбирает её вне потока интерфейса.
     *
     * @return `true` — плитка пришла; `false` — взять её неоткуда, и карта
     *   на её месте остаётся сеткой: точка ставится и без картинки.
     */
    suspend fun fetch(zoom: Int, x: Int, y: Int): Boolean {
        val key = key(zoom, x, y)
        val bytes = readTile(zoom, x, y)
        val bitmap = bytes?.let { withContext(Dispatchers.Default) { encodedImage(it) } }
        if (bitmap == null) failed(key) else arrived(key, bitmap)
        return bitmap != null
    }

    /** Отпускает взятую плитку — пришла она, отказала или загрузку прервали. */
    fun release(zoom: Int, x: Int, y: Int) {
        loading -= key(zoom, x, y)
    }

    private fun arrived(key: String, bitmap: ImageBitmap) {
        TileMemory[key] = bitmap
        failedAt -= key
        version++
        if (blank) blank = false
    }

    private fun failed(key: String) {
        failedAt[key] = TimeSource.Monotonic.markNow()
        failures++
        if (version == 0 && !shown) blank = true
    }

    private fun key(zoom: Int, x: Int, y: Int): String = "${provider.id}/$zoom/$x/$y"

    private companion object {
        /** Пауза перед новым вопросом о плитке, которая не пришла. */
        val RETRY_AFTER = 4.seconds
    }
}
