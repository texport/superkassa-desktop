package kz.mybrain.superkassa.presentation.common.mapview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.domain.map.usecase.ReadTile
import kotlin.test.Test
import kotlin.test.assertFalse
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

    /** Карты, у которых каждый ответ задаёт проверка. */
    private abstract class QuietMapsBase : Maps by QuietMaps()
}
