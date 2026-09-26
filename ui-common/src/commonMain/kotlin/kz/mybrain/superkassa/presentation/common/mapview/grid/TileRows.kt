package kz.mybrain.superkassa.presentation.common.mapview.grid

import kz.mybrain.superkassa.domain.map.model.TileGrid
import kz.mybrain.superkassa.presentation.common.mapview.MapProjection
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/**
 * Строки плиток поставщика в сферической сетке карты.
 *
 * Карта считает всё — центр, метки, касания — в сферическом Меркаторе
 * (EPSG:3857). Плитки большинства поставщиков режут мир так же, и строка
 * плитки — просто её номер на 256 точек. Яндекс режет эллиптическим
 * Меркатором (EPSG:3395): при той же долготе его строка лежит южнее,
 * и плитку ставят туда, где в сферической сетке оказывается её верхний
 * край, — иначе дома на плитке расходились бы с метками на сотни метров.
 */
internal object TileRows {

    /** Верхний край строки [row] сетки [grid] — в точках сферической сетки масштаба [zoom]. */
    fun top(grid: TileGrid, zoom: Int, row: Int): Double = when (grid) {
        TileGrid.WebMercator -> row.toDouble() * MapProjection.TILE
        TileGrid.EllipticalMercator ->
            MapProjection.yOf(ellipticalLatitude(row.toDouble() * MapProjection.TILE, zoom), zoom)
    }

    /** Строка сетки [grid], на которую приходится точка [pixelY] сферической сетки. */
    fun at(grid: TileGrid, zoom: Int, pixelY: Double): Int = when (grid) {
        TileGrid.WebMercator -> MapProjection.tileOf(pixelY)
        TileGrid.EllipticalMercator ->
            floor(ellipticalY(MapProjection.latitudeOf(pixelY, zoom), zoom) / MapProjection.TILE).toInt()
    }

    /** Точка широты [latitude] в эллиптической сетке масштаба [zoom]. */
    fun ellipticalY(latitude: Double, zoom: Int): Double {
        val phi = latitude.coerceIn(-MapProjection.MAX_LATITUDE, MapProjection.MAX_LATITUDE) * PI / HALF_TURN
        val flattening = ((1 - E * sin(phi)) / (1 + E * sin(phi))).pow(E / 2)
        val world = MapProjection.world(zoom)
        return world / 2 - world / (2 * PI) * ln(tan(PI / QUARTER + phi / 2) * flattening)
    }

    /** Широта точки [y] эллиптической сетки — приближениями: обратной формулы у эллипсоида нет. */
    fun ellipticalLatitude(y: Double, zoom: Int): Double {
        val world = MapProjection.world(zoom)
        val t = exp(-(world / 2 - y) * 2 * PI / world)
        var phi = PI / 2 - 2 * atan(t)
        repeat(STEPS) {
            val flattening = ((1 - E * sin(phi)) / (1 + E * sin(phi))).pow(E / 2)
            phi = PI / 2 - 2 * atan(t * flattening)
        }
        return phi * HALF_TURN / PI
    }

    /** Эксцентриситет эллипсоида WGS 84. */
    private const val E = 0.0818191908426

    /** Приближений достаточно, чтобы ошибка стала меньше доли точки. */
    private const val STEPS = 8

    private const val HALF_TURN = 180.0

    /** Четверть: π/4 в формуле Меркатора. */
    private const val QUARTER = 4
}
