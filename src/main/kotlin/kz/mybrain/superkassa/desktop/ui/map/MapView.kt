package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kz.mybrain.superkassa.desktop.ui.strings.MapTexts
import kz.mybrain.superkassa.desktop.ui.theme.MapColors
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.util.concurrent.atomic.AtomicInteger
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
 * Если плитки не пришли — сети нет или служба недоступна, — поле остаётся
 * пустым, и об этом сказано строкой поверх него: серый прямоугольник
 * без объяснения читается как сломанный экран. Точка на нём всё равно
 * ставится: широта и долгота считаются из проекции, а не из картинки.
 */
@Composable
fun MapView(
    state: MapState,
    tiles: MapTiles,
    texts: MapTexts,
    modifier: Modifier = Modifier,
    // Что значит нажатие по карте, решает вызывающий: в окне выбора места
    // оно ставит метку, на карте касс — снимает выбор ярлычка.
    onTap: ((Double, Double) -> Unit)? = null,
    // Ярлычки поверх карты. Стоят внутри её окна, а не рядом: место
    // каждого считается от размера этого окна, и знать его должен тот,
    // кто им владеет.
    overlay: @Composable (IntSize) -> Unit = {}
) {
    var canvas by remember { mutableStateOf(IntSize.Zero) }
    var revision by remember { mutableIntStateOf(0) }
    // Ни одной плитки не доехало: объяснение поверх пустого поля.
    // До первой попытки поле не объясняется — жаловаться ещё не на что.
    var blank by remember { mutableStateOf(false) }
    val wheel = remember { MapWheel() }

    // Плитки берутся сразу несколькими, а не по одной вслед за другой.
    // Прежде они запрашивались подряд, и прокрутка открывала десяток
    // новых плиток одна за другой: пока приходила последняя, владелец
    // смотрел на серое поле секунды. Каждая пришедшая обновляет показ
    // отдельно — ждать всю сетку незачем.
    //
    // Одновременных запросов немного намеренно: плитки отданы сообществом
    // OpenStreetMap, и их правила запрещают массовую выкачку.
    LaunchedEffect(state.zoom, state.centerLatitude, state.centerLongitude, canvas) {
        val wanted = visibleTiles(state, canvas)
        if (wanted.isEmpty()) return@LaunchedEffect
        val gate = Semaphore(TILES_AT_ONCE)
        val arrived = AtomicInteger()
        coroutineScope {
            wanted.forEach { tile ->
                launch {
                    gate.withPermit {
                        if (tiles.fetch(state.zoom, tile.x, tile.y)) {
                            arrived.incrementAndGet()
                            revision += 1
                        }
                    }
                }
            }
        }
        blank = arrived.get() == 0
    }

    val paint = MapPaint(
        chosen = MapColors.chosen,
        located = MapColors.located,
        edge = MapColors.edge,
        halo = MapColors.halo
    )
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
            .pointerInput(state.zoom, canvas, onTap) {
                detectTapGestures { at -> onTap?.let { state.tapped(canvas, at, it) } }
            }
            .pointerInput(state, canvas) { zoomByWheel(state, canvas, wheel) }
    ) {
        MapGlide(state)
        MapCanvas(state, tiles, canvas, revision, paint)
        if (blank) BlankNotice(texts.noTiles, Modifier.align(Alignment.BottomStart).padding(Spacing.snug))
        overlay(canvas)
    }
}

/**
 * Колесо над картой приближает и отдаляет.
 *
 * Слушается само событие прокрутки, а не жест `scrollable`: у карты нет
 * прокручиваемого содержимого, а есть увеличение, и «прокрутить» её
 * значит приблизить. Событие съедается: над картой ему больше ничего
 * двигать не нужно.
 */
private suspend fun PointerInputScope.zoomByWheel(state: MapState, canvas: IntSize, wheel: MapWheel) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.first()
            state.wheeled(canvas, change.position, wheel.turn(change.scrollDelta.y))
            change.consume()
        }
    }
}

/**
 * Почему поле карты пустое.
 *
 * Стоит в нижнем углу, а не в середине: середину занимает метка, и ради
 * объяснения закрывать её нельзя. Заливка поверхности с тенью — иначе
 * надпись теряется на подложке там, где плитки всё-таки доехали.
 */
@Composable
private fun BlankNotice(notice: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Sizes.corner),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Sizes.mapMarkLift
    ) {
        Text(
            text = notice,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.snug, vertical = Spacing.tight)
        )
    }
}

/** Само полотно: плитки, своё место и метка выбранной точки. */
@Composable
private fun MapCanvas(
    state: MapState,
    tiles: MapTiles,
    canvas: IntSize,
    revision: Int,
    paint: MapPaint
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION")
        revision
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
private data class MapPaint(
    val chosen: Color,
    val located: Color,
    val edge: Color,
    val halo: Color
)

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

/** Какие плитки попадают в окно. */
private fun visibleTiles(state: MapState, canvas: IntSize): List<TileIndex> {
    if (canvas.width == 0 || canvas.height == 0) return emptyList()
    val corner = topLeft(state, canvas)
    val edge = MapProjection.tiles(state.zoom)
    val fromX = MapProjection.tileOf(corner.x)
    val fromY = MapProjection.tileOf(corner.y)
    val toX = MapProjection.tileOf(corner.x + canvas.width)
    val toY = MapProjection.tileOf(corner.y + canvas.height)
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

/**
 * Сколько плиток запрашивать одновременно.
 *
 * Больше — быстрее открывается новая область, но плитки отданы
 * сообществом OpenStreetMap, и наваливаться на их службу нельзя.
 */
private const val TILES_AT_ONCE = 6
