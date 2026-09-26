package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.domain.map.model.TileGrid
import kz.mybrain.superkassa.presentation.common.mapview.grid.TileRows
import kotlin.math.roundToInt

/** Само полотно: плитки, своё место и метка выбранной точки. */
@Composable
internal fun MapCanvas(state: MapState, tiles: MapTiles, canvas: IntSize, paint: MapPaint) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawTiles(state, tiles, canvas)
        drawLocation(state, canvas, paint)
        drawMarker(state, canvas, paint)
    }
}

/**
 * Цвета знаков, снятые со схемы.
 *
 * Полотно рисует вне композиции и до схемы не дотягивается: цвета
 * берутся один раз в показе и отдаются рисованию готовыми.
 */
internal data class MapPaint(
    val chosen: Color,
    val located: Color,
    val edge: Color,
    val halo: Color
)

/**
 * Рисует плитки, попадающие в окно, — каждую по сетке её поставщика.
 *
 * Высота плитки берётся из сетки, а не 256 точек: у эллиптической сетки
 * строка в сферической чуть выше или ниже, и без растяжения между
 * строками оставались бы щели в точку. Плитки, которой ещё нет, заменяет
 * четверть плитки предыдущего увеличения, растянутая вдвое, — пока грузится
 * подробная, карта уже видна.
 */
private fun DrawScope.drawTiles(state: MapState, tiles: MapTiles, canvas: IntSize) {
    // Пришедшая плитка меняет счётчик — и полотно перерисовывается.
    if (tiles.version < 0) return
    val corner = topLeft(state, canvas)
    val grid = tiles.provider.grid
    visibleTiles(state, canvas, grid).forEach { tile ->
        val x = (tile.x * MapProjection.TILE - corner.x).roundToInt()
        val top = (TileRows.top(grid, state.zoom, tile.y) - corner.y).roundToInt()
        val bottom = (TileRows.top(grid, state.zoom, tile.y + 1) - corner.y).roundToInt()
        val place = IntSize(MapProjection.TILE, bottom - top)
        val bitmap = tiles.ready(state.zoom, tile.x, tile.y)
        if (bitmap != null) {
            drawImage(bitmap, dstOffset = IntOffset(x, top), dstSize = place)
        } else {
            tiles.cover(state.zoom, tile.x, tile.y)?.let { drawCover(it, tile, IntOffset(x, top), place) }
        }
    }
}

/** Четверть плитки предыдущего увеличения на месте плитки [tile]. */
private fun DrawScope.drawCover(cover: ImageBitmap, tile: TileIndex, at: IntOffset, place: IntSize) {
    val half = MapProjection.TILE / 2
    drawImage(
        cover,
        srcOffset = IntOffset(tile.x % 2 * half, tile.y % 2 * half),
        srcSize = IntSize(half, half),
        dstOffset = at,
        dstSize = place
    )
}

/**
 * Где мы — кружком в ореоле, как это принято в картах.
 *
 * Знак другой, чем у выбранной точки, и намеренно: место определено
 * по адресу подключения и указывает на город, а не на дом. Ореол
 * не меняется с увеличением — он не обещает точности, а показывает,
 * что это именно своё место.
 */
private fun DrawScope.drawLocation(state: MapState, canvas: IntSize, paint: MapPaint) {
    val latitude = state.locationLatitude ?: return
    val longitude = state.locationLongitude ?: return
    val corner = topLeft(state, canvas)
    val at = Offset(
        (MapProjection.xOf(longitude, state.zoom) - corner.x).toFloat(),
        (MapProjection.yOf(latitude, state.zoom) - corner.y).toFloat()
    )
    drawCircle(paint.halo, radius = Sizes.mapHalo.toPx(), center = at)
    drawCircle(paint.edge, radius = (Sizes.mapLocation + Sizes.mapMarkerEdge).toPx(), center = at)
    drawCircle(paint.located, radius = Sizes.mapLocation.toPx(), center = at)
}

/** Метка выбранной точки: кружок с обводкой, видимый на любой подложке. */
private fun DrawScope.drawMarker(state: MapState, canvas: IntSize, paint: MapPaint) {
    val latitude = state.markerLatitude ?: return
    val longitude = state.markerLongitude ?: return
    val corner = topLeft(state, canvas)
    val x = MapProjection.xOf(longitude, state.zoom) - corner.x
    val y = MapProjection.yOf(latitude, state.zoom) - corner.y
    val at = Offset(x.toFloat(), y.toFloat())
    drawCircle(paint.edge, radius = (Sizes.mapMarker + Sizes.mapMarkerEdge).toPx(), center = at)
    drawCircle(paint.chosen, radius = Sizes.mapMarker.toPx(), center = at)
}

/** Точка полотна мира, попавшая в левый верхний угол окна. */
private fun topLeft(state: MapState, canvas: IntSize): MapPixel =
    MapProjection.corner(state.centerLatitude, state.centerLongitude, state.zoom, canvas.width, canvas.height)

/** Какие плитки сетки [grid] попадают в окно. */
internal fun visibleTiles(state: MapState, canvas: IntSize, grid: TileGrid = TileGrid.WebMercator): List<TileIndex> {
    if (canvas.width == 0 || canvas.height == 0) return emptyList()
    val corner = topLeft(state, canvas)
    val edge = MapProjection.tiles(state.zoom)
    val fromX = MapProjection.tileOf(corner.x)
    val fromY = TileRows.at(grid, state.zoom, corner.y)
    val toX = MapProjection.tileOf(corner.x + canvas.width)
    val toY = TileRows.at(grid, state.zoom, corner.y + canvas.height)
    val tiles = mutableListOf<TileIndex>()
    for (y in fromY..toY) {
        for (x in fromX..toX) {
            if (x in 0 until edge && y in 0 until edge) tiles += TileIndex(x, y)
        }
    }
    return tiles
}

/** Номер плитки в сетке мира. */
internal data class TileIndex(val x: Int, val y: Int)
