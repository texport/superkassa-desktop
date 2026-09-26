package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kz.mybrain.superkassa.designsystem.image.encodedImage
import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.usecase.ReadTile
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Плитки карты, готовые к рисованию, — одного поставщика на время одной карты.
 *
 * Где плитку взять и где её хранить, решает сценарий [ReadTile]: диск
 * рабочего места, потом сеть. Здесь только картинки для полотна — и лежат
 * они в состоянии Compose: пришедшая плитка сама перерисовывает карту.
 *
 * Не пришедшая плитка не остаётся дырой навсегда: её спрашивают снова,
 * но не чаще раза в [RETRY_AFTER] — служба, отказавшая только что, сразу
 * не ответит. Прерванная загрузка неудачей не считается: владелец сдвинул
 * карту, и плитка просто спросится, когда снова попадёт в окно. Прежде
 * и то и другое заносило плитку в «не пришедшие» до закрытия карты,
 * и на карте оставались серые квадраты.
 *
 * Спрашиваются плитки из показа карты, одним потоком интерфейса: своей
 * защиты от одновременных обращений учёту не нужно.
 *
 * @property provider чьи плитки: по его сетке они ставятся, его подпись стоит в углу.
 */
class MapTiles(private val readTile: ReadTile, val provider: MapProvider = MapProvider.OpenStreetMap) {

    private val images = mutableStateMapOf<String, ImageBitmap>()
    private val loading = mutableSetOf<String>()
    private val failedAt = mutableMapOf<String, TimeMark>()

    /** Сколько раз плитка не пришла; меняется — показ пересматривает, что спросить. */
    var failures: Int by mutableIntStateOf(0)
        private set

    /** Ни одна плитка не пришла, а неудачи были: поле карты надо объяснить. */
    val blank: Boolean get() = failures > 0 && images.isEmpty()

    /** Плитка, если она уже под рукой. Показ рисует только то, что есть. */
    fun ready(zoom: Int, x: Int, y: Int): ImageBitmap? = images[key(zoom, x, y)]

    /**
     * Берёт плитку в работу: `true` — её нет, она не грузится и не отказала
     * только что. Взятую плитку показ грузит [fetch] и отпускает [release].
     */
    fun claim(zoom: Int, x: Int, y: Int): Boolean {
        val key = key(zoom, x, y)
        val recent = failedAt[key]?.let { it.elapsedNow() < RETRY_AFTER } == true
        if (key in images || key in loading || recent) return false
        loading += key
        return true
    }

    /**
     * Достаёт плитку у службы карт.
     *
     * @return `true` — плитка пришла; `false` — взять её неоткуда, и карта
     *   на её месте остаётся сеткой: точка ставится и без картинки.
     */
    suspend fun fetch(zoom: Int, x: Int, y: Int): Boolean {
        val key = key(zoom, x, y)
        val bitmap = readTile(zoom, x, y)?.let(::encodedImage)
        if (bitmap == null) {
            failedAt[key] = TimeSource.Monotonic.markNow()
            failures++
        } else {
            images[key] = bitmap
            failedAt -= key
        }
        return bitmap != null
    }

    /** Отпускает взятую плитку — пришла она, отказала или загрузку прервали. */
    fun release(zoom: Int, x: Int, y: Int) {
        loading -= key(zoom, x, y)
    }

    private fun key(zoom: Int, x: Int, y: Int): String = "$zoom/$x/$y"

    private companion object {
        /** Пауза перед новым вопросом о плитке, которая не пришла. */
        val RETRY_AFTER = 4.seconds
    }
}
