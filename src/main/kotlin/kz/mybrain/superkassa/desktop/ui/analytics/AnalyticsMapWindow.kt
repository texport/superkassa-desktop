package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.map.MapControls
import kz.mybrain.superkassa.desktop.ui.map.MapMarks
import kz.mybrain.superkassa.desktop.ui.map.MapView
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Само окно карты касс: плитки, ярлычки мест и управление в углу.
 *
 * Одно и то же в разделе и во всём окне: карта не должна вести себя
 * по-разному оттого, сколько места ей отдано. Различие одно — кнопка
 * в углу: из раздела она раскрывает карту во всё окно, из всего окна
 * возвращает в раздел.
 *
 * Пока ни одной точки нет, карта заменяется объяснением: пустая карта
 * города говорит владельцу не больше, чем пустой экран.
 *
 * @param fullscreen занимает ли карта сейчас всё окно.
 * @param onFullscreen раскрыть карту во всё окно или вернуть в раздел.
 */
@Composable
internal fun MapWindow(
    parts: MapParts,
    fullscreen: Boolean,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = parts.model
    if (parts.placement.placed.isEmpty()) {
        val reason = emptyMapReason(parts.placement, model.sieve.set, parts.texts)
        EmptyState(
            icon = reason.icon,
            title = reason.title,
            hint = reason.hint,
            modifier = modifier,
            centered = true
        )
        return
    }
    Box(modifier = modifier) {
        MapView(
            state = model.map,
            tiles = parts.services.tiles,
            texts = parts.cabinetTexts.map,
            modifier = Modifier.fillMaxSize(),
            // Нажатие мимо ярлычка снимает выбор: раскрытое место
            // закрывается тем же способом, каким открылось.
            onTap = { _, _ -> model.forget() },
            overlay = { canvas -> MapOverlay(parts, canvas) }
        )
        MapControls(
            state = model.map,
            texts = parts.cabinetTexts,
            preferences = parts.session.preferences,
            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.snug)
        ) {
            IconButton(onClick = onFullscreen) {
                Icon(
                    imageVector = if (fullscreen) AppIcons.fullscreenExit else AppIcons.fullscreen,
                    contentDescription = if (fullscreen) parts.texts.mapFullscreenExit else parts.texts.mapFullscreen
                )
            }
        }
    }
}

/**
 * Что лежит поверх плиток: ярлычки мест, итог по видимому куску и легенда.
 *
 * Места отсеиваются по окну до того, как сложится хоть один ярлычок,
 * и тем же отбором считается итог: на карте написано ровно то число,
 * которое владелец сейчас видит кружками.
 *
 * Итог слева сверху, легенда слева снизу: справа стоит управление картой,
 * а угол под ним занимает объяснение о неприехавших плитках.
 */
@Composable
private fun MapOverlay(parts: MapParts, canvas: IntSize) {
    val model = parts.model
    val shown = onScreen(parts.groups, model.map, canvas)
    MapMarks(model.map, canvas, kkmMarks(shown, model)) { mark ->
        model.open(parts.groups.first { it.id == mark.id })
    }
    Box(Modifier.fillMaxSize()) {
        MapTally(
            shown = mapCount(shown, parts.placement.placed.size, parts.whole),
            sieved = model.sieve.set,
            texts = parts.texts,
            modifier = Modifier.align(Alignment.TopStart).padding(Spacing.snug)
        )
        MapLegend(parts.legend, parts.texts, Modifier.align(Alignment.BottomEnd).padding(Spacing.snug))
    }
}
