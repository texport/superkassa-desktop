package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kz.mybrain.superkassa.designsystem.theme.MapColors
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.mapview.control.MapNotes
import kz.mybrain.superkassa.strings.api.map.MapTexts

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
 * Не пришедшие плитки спрашиваются снова (см. [MapTiles]).
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
    val wheel = remember { MapWheel() }
    LoadTiles(state, tiles) { canvas }

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
        MapCanvas(state, tiles, canvas, paint)
        MapNotes(
            blank = tiles.blank,
            attribution = tiles.provider.attribution,
            texts = texts,
            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.fieldGap)
        )
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
 * Загрузка плиток, попадающих в окно, — одна на всё время карты.
 *
 * Прежде загрузка перезапускалась на каждом сдвиге центра и обрывала
 * плитки в пути: при перетаскивании карты они не доходили никогда, а
 * оборванные числились не пришедшими. Теперь новые плитки берутся в работу,
 * как только попадают в окно, а начатые доходят до конца. Плитки берутся
 * сразу несколькими — каждая пришедшая обновляет показ отдельно, — но
 * одновременных запросов немного: плитки отданы сообществом OpenStreetMap,
 * и его правила запрещают массовую выкачку.
 */
@Composable
private fun LoadTiles(state: MapState, tiles: MapTiles, canvas: () -> IntSize) {
    LaunchedEffect(tiles) {
        val gate = Semaphore(TILES_AT_ONCE)
        snapshotFlow { Wanted(state.zoom, visibleTiles(state, canvas(), tiles.provider.grid), tiles.failures) }
            .collect { wanted ->
                wanted.tiles.filter { tiles.claim(wanted.zoom, it.x, it.y) }.forEach { tile ->
                    launch { load(tiles, gate, wanted.zoom, tile) }
                }
            }
    }
}

/** Одна плитка: загрузка под общим ограничением и отпуск в любом исходе. */
private suspend fun load(tiles: MapTiles, gate: Semaphore, zoom: Int, tile: TileIndex) {
    try {
        gate.withPermit { tiles.fetch(zoom, tile.x, tile.y) }
    } finally {
        tiles.release(zoom, tile.x, tile.y)
    }
}

/** Что показ хочет видеть: плитки окна при этом увеличении и число неудач — повод спросить снова. */
private data class Wanted(val zoom: Int, val tiles: List<TileIndex>, val failures: Int)

/**
 * Сколько плиток запрашивать одновременно.
 *
 * Больше — быстрее открывается новая область, но плитки отданы
 * сообществом OpenStreetMap, и наваливаться на их службу нельзя.
 */
private const val TILES_AT_ONCE = 6
