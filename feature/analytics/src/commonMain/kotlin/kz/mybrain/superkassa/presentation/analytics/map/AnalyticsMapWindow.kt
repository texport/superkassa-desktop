package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.analytics.map.component.MapLegend
import kz.mybrain.superkassa.presentation.analytics.map.component.TallyLine
import kz.mybrain.superkassa.presentation.analytics.map.component.emptyMapReason
import kz.mybrain.superkassa.presentation.analytics.map.component.mapCount
import kz.mybrain.superkassa.presentation.common.mapview.MapControls
import kz.mybrain.superkassa.presentation.common.mapview.MapMarks
import kz.mybrain.superkassa.presentation.common.mapview.MapView
import kz.mybrain.superkassa.presentation.common.mapview.control.FullscreenButton

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
    // Сколько высоты занимает столбик кнопок: легенда под ним не выше остатка.
    var controls by remember { mutableIntStateOf(0) }
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
            overlay = { canvas -> MapOverlay(parts, canvas, controls) }
        )
        MapControls(
            state = model.map,
            texts = parts.cabinetTexts,
            locating = parts.tools.locating,
            modifier = Modifier.align(Alignment.TopEnd).onSizeChanged { controls = it.height }.padding(Spacing.fieldGap)
        ) {
            // Та же кнопка, что у окна выбора места: одна на все карты.
            FullscreenButton(fullscreen, parts.cabinetTexts.map, onFullscreen)
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
 * Управление — в правом верхнем углу, легенда — в правом нижнем, под
 * кнопками: так она не отнимает у карты тот угол, с которого глаз начинает
 * читать. Левый нижний угол занят объяснением о неприехавших плитках.
 * Итог по видимому куску карты — первой строкой раскрытой легенды:
 * отдельной плашкой поверх карты он закрывал её угол, а в узком окне
 * растягивался на всю ширину и закрывал карту целиком.
 */
@Composable
private fun MapOverlay(parts: MapParts, canvas: IntSize, controls: Int) {
    val model = parts.state
    val shown = onScreen(parts.groups, model.map, canvas)
    MapMarks(model.map, canvas, kkmMarks(shown, model), groupCell(LocalDensity.current.density)) { mark ->
        parts.actions.open(parts.groups.first { it.id == mark.id })
    }
    val count = mapCount(shown, parts.placement.placed.size, parts.whole)
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(Spacing.fieldGap)) {
        // Не выше того, что остаётся под кнопками карты: в невысоком окне
        // раскрытая легенда ложилась на «Во весь экран». В самой низкой
        // карте ей остаётся хотя бы треть — дальше строки прокручиваются.
        val under = maxHeight - with(LocalDensity.current) { controls.toDp() }
        val room = Modifier.align(Alignment.BottomEnd).heightIn(max = maxOf(under, maxHeight / LEGEND_LEAST))
        MapLegend(parts.tools.legend, parts.texts, room) {
            TallyLine(count, model.sieve.set, parts.texts)
        }
    }
}

/** Меньше какой доли карты легенда не становится: трети. */
private const val LEGEND_LEAST = 3

/**
 * Метка окна карты для проверок раскладки.
 *
 * Своей надписи у полотна карты нет, а мерить приходится именно его:
 * высоту карты в малом окне и на планшете.
 */
const val MAP_TAG = "analytics-map"
