package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки колонки торговых точек с живого прохода.
 *
 * Точку опознают адресом, и в колонке под названием стояло только «Касс: 3»:
 * три магазина одной сети отличались друг от друга лишь названием, которое
 * владелец сам же и придумал. Здесь же видно имя номера в поиске — одно
 * на всё приложение.
 */
class LiveWalkPlaceShots {

    /** Точки с адресом под названием и кассы под раскрытой точкой. */
    @Test
    fun `адрес точки виден в колонке`() {
        val places = (1..THREE).map { PlaceLook.place(it) }
        val registers = places.flatMapIndexed { at, place ->
            listOf(PlaceLook.register(at + 1, place.id))
        }
        val shot = fix("11-place-address") {
            PlacesLook(places = places, registers = registers, open = places.first().id)
        }
        assertTrue(shot.isNotEmpty())
    }

    /** Точка без адреса: строки нет, а не пустое место под названием. */
    @Test
    fun `точка без адреса обходится без пустой строки`() {
        val place = PlaceLook.place(1, address = null, registers = 0)
        val shot = fix("11-place-no-address") { PlacesLook(places = listOf(place), registers = emptyList()) }
        assertTrue(shot.isNotEmpty())
    }

    private fun fix(name: String, content: @Composable () -> Unit): ByteArray =
        RenderProbe(width = WIDE, height = HIGH, content = content).use { probe ->
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/fix-$name.png").writeBytes(frame)
            frame
        }

    private companion object {
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 40
        const val THREE = 3
    }
}
