package kz.mybrain.superkassa.presentation.common.mapview.grid

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Разобранные плитки в памяти — одни на всё приложение.
 *
 * Карта открывается снова и снова: владелец уходит из раздела и
 * возвращается, открывает выбор места у каждой точки. Без общей памяти
 * каждое открытие заново читало плитки с диска и заново разбирало PNG,
 * и знакомая карта рисовалась так же долго, как в первый раз.
 *
 * Память ограничена: плитка в памяти — 256 × 256 точек по четыре байта,
 * четверть мегабайта, и на планшете держать их без счёта нельзя. Вытесняется
 * та, что дольше всех не была нужна. Обращаются к памяти только из потока
 * интерфейса: своей защиты от одновременных обращений ей не нужно.
 */
internal object TileMemory {
    private val images = LinkedHashMap<String, ImageBitmap>()

    /** Плитка, если она в памяти; найденная становится самой свежей. */
    operator fun get(key: String): ImageBitmap? {
        val image = images.remove(key) ?: return null
        images[key] = image
        return image
    }

    /** Кладёт плитку; сверх предела вытесняет самую давнюю. */
    operator fun set(key: String, image: ImageBitmap) {
        images.remove(key)
        images[key] = image
        while (images.size > CAPACITY) images.remove(images.keys.first())
    }

    /** Есть ли плитка в памяти — без обновления её свежести. */
    operator fun contains(key: String): Boolean = key in images

    /** Сколько плиток в памяти сейчас. */
    val size: Int get() = images.size

    /**
     * Сколько плиток держать: окно планшета в 2560 × 1600 точек — около
     * восьмидесяти плиток с запасом по краям; двадцать четыре мегабайта.
     */
    const val CAPACITY: Int = 96
}
