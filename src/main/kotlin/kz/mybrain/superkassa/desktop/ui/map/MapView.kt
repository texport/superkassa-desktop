package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Карта с точкой: владелец ставит место торговой точки нажатием.
 *
 * Координаты в кабинете обязательны, а в адресном регистре их нет —
 * прежде владелец набирал широту и долготу руками, взяв их неизвестно
 * откуда. Карта отвечает на тот же вопрос глазами: вот дом, вот точка.
 *
 * Показ намеренно простой: плитки рисуются как есть, увеличение целое,
 * без промежуточных состояний. Карта здесь — способ указать точку, а не
 * навигатор, и вращения с наклоном ей ни к чему.
 *
 * Если плитки не пришли — сети нет или служба недоступна, — остаётся
 * пустое поле, и точка на нём всё равно ставится: широта и долгота
 * считаются из проекции, а не из картинки.
 */
@Composable
fun MapView(state: MapState, tiles: MapTiles, modifier: Modifier = Modifier) {
    var canvas by remember { mutableStateOf(IntSize.Zero) }
    var revision by remember { mutableIntStateOf(0) }

    // Плитки приходят по одной и каждая обновляет показ: рисовать всё
    // разом значило бы держать пустое поле, пока грузится последняя.
    LaunchedEffect(state.zoom, state.centerLatitude, state.centerLongitude, canvas) {
        visibleTiles(state, canvas).forEach { tile ->
            if (tiles.fetch(state.zoom, tile.x, tile.y)) revision += 1
        }
    }

    Box(
        modifier = modifier
            // Плитки рисуются целиком, и крайние выходят за окно карты.
            // Полотно Compose само их не обрезает: без этого карта
            // закрашивала шапку окна сверху и подпись с кнопками снизу.
            .clipToBounds()
            .onSizeChanged { canvas = it }
            .background(MapColors.empty)
            .pointerInput(state.zoom) {
                detectDragGestures { _, dragged -> state.pan(dragged.x, dragged.y) }
            }
            .pointerInput(state.zoom, canvas) {
                detectTapGestures { at ->
                    val world = worldOf(state, canvas, at)
                    state.mark(
                        MapProjection.latitudeOf(world.y.toDouble(), state.zoom),
                        MapProjection.longitudeOf(world.x.toDouble(), state.zoom)
                    )
                }
            }
    ) {
        MapCanvas(state, tiles, canvas, revision)
    }
}

/** Само полотно: плитки, сетка на месте недошедших и метка выбранной точки. */
@Composable
private fun MapCanvas(state: MapState, tiles: MapTiles, canvas: IntSize, revision: Int) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        revision
        drawTiles(state, tiles, canvas)
        drawLocation(state, canvas)
        drawMarker(state, canvas)
    }
}

/** Рисует плитки, попадающие в окно. */
private fun DrawScope.drawTiles(state: MapState, tiles: MapTiles, canvas: IntSize) {
    val corner = topLeft(state, canvas)
    visibleTiles(state, canvas).forEach { tile ->
        val bitmap = tiles.ready(state.zoom, tile.x, tile.y) ?: return@forEach
        val x = (tile.x * MapProjection.TILE - corner.x).roundToInt()
        val y = (tile.y * MapProjection.TILE - corner.y).roundToInt()
        drawImage(bitmap, dstOffset = IntOffset(x, y))
    }
}

/**
 * Где мы — синим кружком в ореоле, как это принято в картах.
 *
 * Знак другой, чем у выбранной точки, и намеренно: место определено
 * по адресу подключения и указывает на город, а не на дом. Ореол
 * не меняется с увеличением — он не обещает точности, а показывает,
 * что это именно своё место.
 */
private fun DrawScope.drawLocation(state: MapState, canvas: IntSize) {
    val latitude = state.locationLatitude ?: return
    val longitude = state.locationLongitude ?: return
    val corner = topLeft(state, canvas)
    val at = Offset(
        (MapProjection.xOf(longitude, state.zoom) - corner.x).toFloat(),
        (MapProjection.yOf(latitude, state.zoom) - corner.y).toFloat()
    )
    drawCircle(MapColors.locationHalo, radius = LOCATION_HALO, center = at)
    drawCircle(MapColors.markerEdge, radius = LOCATION_RADIUS + MARKER_EDGE, center = at)
    drawCircle(MapColors.location, radius = LOCATION_RADIUS, center = at)
}

/** Метка выбранной точки: кружок с обводкой, видимый на любой подложке. */
private fun DrawScope.drawMarker(state: MapState, canvas: IntSize) {
    val latitude = state.markerLatitude ?: return
    val longitude = state.markerLongitude ?: return
    val corner = topLeft(state, canvas)
    val x = MapProjection.xOf(longitude, state.zoom) - corner.x
    val y = MapProjection.yOf(latitude, state.zoom) - corner.y
    val at = Offset(x.toFloat(), y.toFloat())
    drawCircle(MapColors.markerEdge, radius = MARKER_RADIUS + MARKER_EDGE, center = at)
    drawCircle(MapColors.marker, radius = MARKER_RADIUS, center = at)
}

/** Точка полотна мира, попавшая в левый верхний угол окна. */
private fun topLeft(state: MapState, canvas: IntSize): Offset {
    val centerX = MapProjection.xOf(state.centerLongitude, state.zoom)
    val centerY = MapProjection.yOf(state.centerLatitude, state.zoom)
    return Offset((centerX - canvas.width / 2.0).toFloat(), (centerY - canvas.height / 2.0).toFloat())
}

/** Точка полотна мира под нажатием. */
private fun worldOf(state: MapState, canvas: IntSize, at: Offset): Offset {
    val corner = topLeft(state, canvas)
    return Offset(corner.x + at.x, corner.y + at.y)
}

/** Какие плитки попадают в окно. */
private fun visibleTiles(state: MapState, canvas: IntSize): List<TileIndex> {
    if (canvas.width == 0 || canvas.height == 0) return emptyList()
    val corner = topLeft(state, canvas)
    val edge = MapProjection.tiles(state.zoom)
    val fromX = MapProjection.tileOf(corner.x.toDouble())
    val fromY = MapProjection.tileOf(corner.y.toDouble())
    val toX = MapProjection.tileOf(corner.x + canvas.width.toDouble())
    val toY = MapProjection.tileOf(corner.y + canvas.height.toDouble())
    val tiles = mutableListOf<TileIndex>()
    for (y in fromY..toY) {
        for (x in fromX..toX) {
            if (x in 0 until edge && y in 0 until edge) tiles += TileIndex(x, y)
        }
    }
    return tiles
}

/** Номер плитки в сетке мира. */
private data class TileIndex(val x: Int, val y: Int)

/** Цвета карты: подложка под недошедшие плитки и метка. */
private object MapColors {
    val empty = Color(0xFFE8E8E4)
    val marker = Color(0xFFD32F2F)
    val markerEdge = Color(0xFFFFFFFF)
    val location = Color(0xFF1A73E8)
    val locationHalo = Color(0x331A73E8)
}

private const val MARKER_RADIUS = 7f
private const val MARKER_EDGE = 3f
private const val LOCATION_RADIUS = 6f
private const val LOCATION_HALO = 22f
