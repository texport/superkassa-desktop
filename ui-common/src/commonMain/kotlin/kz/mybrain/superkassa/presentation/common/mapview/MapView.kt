package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kz.mybrain.superkassa.designsystem.theme.MapColors
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
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
        // Плитки спрашиваются в потоке показа по очереди его точек
        // приостановки: счётчику своя защита не нужна.
        var arrived = 0
        coroutineScope {
            wanted.forEach { tile ->
                launch {
                    gate.withPermit {
                        if (tiles.fetch(state.zoom, tile.x, tile.y)) arrived++
                    }
                }
            }
        }
        blank = arrived == 0
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
        MapCanvas(state, tiles, canvas, paint)
        if (blank) BlankNotice(texts.noTiles, Modifier.align(Alignment.BottomStart).padding(Spacing.fieldGap))
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
            modifier = Modifier.padding(horizontal = Spacing.fieldGap, vertical = Spacing.itemGap)
        )
    }
}

/**
 * Сколько плиток запрашивать одновременно.
 *
 * Больше — быстрее открывается новая область, но плитки отданы
 * сообществом OpenStreetMap, и наваливаться на их службу нельзя.
 */
private const val TILES_AT_ONCE = 6
