package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Box
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
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.analytics.map.component.MapLegend
import kz.mybrain.superkassa.presentation.analytics.map.component.MapTally
import kz.mybrain.superkassa.presentation.analytics.map.component.emptyMapReason
import kz.mybrain.superkassa.presentation.analytics.map.component.mapCount
import kz.mybrain.superkassa.presentation.common.mapview.MapControls
import kz.mybrain.superkassa.presentation.common.mapview.MapMarks
import kz.mybrain.superkassa.presentation.common.mapview.MapView

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
 * Итог — в левом верхнем углу, управление — в правом верхнем, легенда —
 * в правом нижнем, под кнопками: так она не отнимает у карты тот угол,
 * с которого глаз начинает читать, и не ложится на кружки у левого края.
 * Кнопка «Во весь экран» стоит в столбике управления сверху и с легендой
 * не встречается. Левый нижний угол занят объяснением о неприехавших
 * плитках. Итог и легенда сворачиваются заголовком, как карточка под
 * картой: что свёрнуто, помнит рабочее место.
 */
@Composable
private fun MapOverlay(parts: MapParts, canvas: IntSize) {
    val model = parts.state
    val shown = onScreen(parts.groups, model.map, canvas)
    MapMarks(model.map, canvas, kkmMarks(shown, model), groupCell(LocalDensity.current.density)) { mark ->
        parts.actions.open(parts.groups.first { it.id == mark.id })
    }
    Box(modifier = Modifier.fillMaxSize().padding(Spacing.fieldGap)) {
        MapTally(
            shown = mapCount(shown, parts.placement.placed.size, parts.whole),
            sieved = model.sieve.set,
            fold = parts.tools.tally,
            texts = parts.texts,
            modifier = Modifier.align(Alignment.TopStart)
        )
        MapLegend(parts.tools.legend, parts.texts, Modifier.align(Alignment.BottomEnd))
    }
}

/**
 * Метка окна карты для проверок раскладки.
 *
 * Своей надписи у полотна карты нет, а мерить приходится именно его:
 * высоту карты в малом окне и на планшете.
 */
const val MAP_TAG = "analytics-map"
