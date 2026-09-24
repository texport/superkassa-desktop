package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import kz.mybrain.superkassa.designsystem.image.encodedImage
import kz.mybrain.superkassa.domain.map.usecase.ReadTile

/**
 * Плитки карты, готовые к рисованию.
 *
 * Где плитку взять и где её хранить, решает сценарий [ReadTile]: диск
 * рабочего места, потом сеть. Здесь только картинки для полотна — и лежат
 * они в состоянии Compose: пришедшая плитка сама перерисовывает карту.
 * Прежде картинки лежали вне него, и полотно приходилось будить
 * искусственным счётчиком, который читался ради одного побочного действия.
 *
 * Спрашивается плитка из показа карты, одним потоком интерфейса: своей
 * защиты от одновременных обращений ей не нужно.
 */
class MapTiles(private val readTile: ReadTile) {

    private val images = mutableStateMapOf<String, ImageBitmap>()
    private val missing = mutableSetOf<String>()

    /** Плитка, если она уже под рукой. Показ рисует только то, что есть. */
    fun ready(zoom: Int, x: Int, y: Int): ImageBitmap? = images[key(zoom, x, y)]

    /**
     * Достаёт плитку у службы карт.
     *
     * Возвращает `false`, если плитки нет и взять её неоткуда, — карта
     * в этом случае остаётся сеткой, а выбор точки продолжает работать:
     * координаты считаются из проекции, а не из картинки. Не пришедшая
     * плитка до закрытия карты больше не спрашивается.
     */
    suspend fun fetch(zoom: Int, x: Int, y: Int): Boolean {
        val key = key(zoom, x, y)
        if (images.containsKey(key) || key in missing) return images.containsKey(key)
        val bitmap = readTile(zoom, x, y)?.let(::encodedImage)
        if (bitmap == null) missing += key else images[key] = bitmap
        return bitmap != null
    }

    private fun key(zoom: Int, x: Int, y: Int): String = "$zoom/$x/$y"
}
