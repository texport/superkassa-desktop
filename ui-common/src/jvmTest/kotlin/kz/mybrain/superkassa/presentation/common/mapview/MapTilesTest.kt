package kz.mybrain.superkassa.presentation.common.mapview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.model.MapProvider
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.domain.map.usecase.ReadTile
import kz.mybrain.superkassa.presentation.common.mapview.grid.TileMemory
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Плитки не остаются серыми дырами.
 *
 * Прежде не пришедшая плитка — и прерванная сдвигом карты, и оборванная
 * связью — не спрашивалась до закрытия карты.
 */
class MapTilesTest {

    @Test
    fun `прерванная загрузка не считается отказом`() = runTest {
        val never = CompletableDeferred<ByteArray?>()
        val waiting = object : QuietMapsBase() {
            override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = never.await()
        }
        val tiles = MapTiles(ReadTile(waiting))
        assertTrue(tiles.claim(1, 0, 0))
        val loading = async(start = CoroutineStart.UNDISPATCHED) {
            try {
                tiles.fetch(1, 0, 0)
            } finally {
                tiles.release(1, 0, 0)
            }
        }
        assertFalse(tiles.claim(1, 0, 0), "плитка в пути берётся второй раз")
        loading.cancel()
        loading.join()
        assertTrue(tiles.claim(1, 0, 0), "прерванная плитка не спрашивается снова")
        assertFalse(tiles.blank)
    }

    @Test
    fun `не пришедшая плитка ждёт паузу, а не закрытия карты`() = runTest {
        val tiles = MapTiles(ReadTile(QuietMaps()))
        assertTrue(tiles.claim(1, 0, 0))
        assertFalse(tiles.fetch(1, 0, 0))
        tiles.release(1, 0, 0)
        assertTrue(tiles.blank, "пустое поле не объяснено")
        assertFalse(tiles.claim(1, 0, 0), "отказавшую только что спрашивают сразу")
    }

    /**
     * Открытая снова карта не читает плитку заново: разобранная картинка
     * лежит в общей памяти, и знакомая карта рисуется сразу.
     */
    @Test
    fun `вторая карта берёт плитку из памяти, не спрашивая службу`() = runTest {
        var asked = 0
        val counting = object : QuietMapsBase() {
            override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = PNG.also { asked++ }
        }
        val first = MapTiles(ReadTile(counting), provider)
        assertTrue(first.claim(ZOOM, 7, 9))
        assertTrue(first.fetch(ZOOM, 7, 9))
        first.release(ZOOM, 7, 9)

        val second = MapTiles(ReadTile(counting), provider)
        assertFalse(second.claim(ZOOM, 7, 9), "плитка из памяти спрошена снова")
        assertNotNull(second.ready(ZOOM, 7, 9))
        assertEquals(1, asked)
    }

    /** Пока подробная плитка не пришла, её место занимает четверть плитки помельче. */
    @Test
    fun `приближенная карта сразу показывает плитку помельче`() = runTest {
        val tiles = MapTiles(ReadTile(pictured()), provider)
        tiles.claim(ZOOM, 3, 5)
        tiles.fetch(ZOOM, 3, 5)

        assertNull(tiles.ready(ZOOM + 1, 7, 11))
        assertNotNull(tiles.cover(ZOOM + 1, 7, 11), "у подробной плитки нет подложки")
    }

    /** Общая память ограничена: плитки не копятся без счёта. */
    @Test
    fun `память плиток ограничена`() = runTest {
        val tiles = MapTiles(ReadTile(pictured()), provider)
        repeat(TileMemory.CAPACITY + 10) { x ->
            tiles.claim(ZOOM, x, 0)
            tiles.fetch(ZOOM, x, 0)
        }
        assertTrue(TileMemory.size <= TileMemory.CAPACITY)
        assertNotNull(tiles.ready(ZOOM, TileMemory.CAPACITY + 9, 0), "свежая плитка вытеснена")
    }

    /** Свой поставщик на проверку: плитки в общей памяти не пересекаются с другими проверками. */
    private val provider = MapProvider(id = "test-${System.nanoTime()}", name = "Test", attribution = "©")

    /** Карты, отдающие на всякую плитку одну картинку. */
    private fun pictured(): Maps = object : QuietMapsBase() {
        override suspend fun tile(zoom: Int, x: Int, y: Int): ByteArray? = PNG
    }

    /** Карты, у которых каждый ответ задаёт проверка. */
    private abstract class QuietMapsBase : Maps by QuietMaps()

    private companion object {
        const val ZOOM = 12

        /** Плитка в одну точку — настоящий PNG, который разбирает Skia. */
        val PNG: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
        )
    }
}
