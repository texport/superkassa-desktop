package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.presentation.analytics.map.component.MapLegend
import kz.mybrain.superkassa.presentation.analytics.map.component.MapTally
import kz.mybrain.superkassa.presentation.analytics.map.component.emptyMapReason
import kz.mybrain.superkassa.presentation.analytics.map.component.mapCount
import kz.mybrain.superkassa.presentation.common.mapview.MapControls
import kz.mybrain.superkassa.presentation.common.mapview.MapMarks
import kz.mybrain.superkassa.presentation.common.mapview.MapView
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
    val model = parts.state
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
    Box(modifier = modifier.testTag(MAP_TAG)) {
        MapView(
            state = model.map,
            tiles = parts.tools.tiles,
            // О неприехавших плитках — свои слова: обещание окна выбора места
            // «место ставится нажатием» на карте касс было неправдой.
            texts = parts.cabinetTexts.map.copy(noTiles = parts.texts.mapNoTiles),
            modifier = Modifier.fillMaxSize(),
            // Нажатие мимо ярлычка снимает выбор: раскрытое место
            // закрывается тем же способом, каким открылось.
            onTap = { _, _ -> parts.actions.forget() },
            overlay = { canvas -> MapOverlay(parts, canvas) }
        )
        MapControls(
            state = model.map,
            texts = parts.cabinetTexts,
            locating = parts.tools.locating,
            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.fieldGap)
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
 * Итог и легенда — столбиком в левом верхнем углу, управление — в правом.
 * Легенда стояла в правом нижнем, и в низкой карте кнопка «Во весь экран»
 * ложилась на её угол. Левый нижний занят объяснением о неприехавших
 * плитках. Столбик слева и кнопки справа не встречаются, пока карта
 * не уже [kz.mybrain.superkassa.presentation.theme.size.Panes.mapAndDetails].
 */
@Composable
private fun MapOverlay(parts: MapParts, canvas: IntSize) {
    val model = parts.state
    val shown = onScreen(parts.groups, model.map, canvas)
    MapMarks(model.map, canvas, kkmMarks(shown, model), groupCell(LocalDensity.current.density)) { mark ->
        parts.actions.open(parts.groups.first { it.id == mark.id })
    }
    Column(
        modifier = Modifier.padding(Spacing.fieldGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        MapTally(
            shown = mapCount(shown, parts.placement.placed.size, parts.whole),
            sieved = model.sieve.set,
            texts = parts.texts
        )
        MapLegend(parts.tools.legend, parts.texts)
    }
}

/**
 * Метка окна карты для проверок раскладки.
 *
 * Своей надписи у полотна карты нет, а мерить приходится именно его:
 * высоту карты в малом окне и на планшете.
 */
internal const val MAP_TAG = "analytics-map"
