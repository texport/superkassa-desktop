package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.Placement
import kz.mybrain.superkassa.domain.analytics.model.PlacementTrouble
import kz.mybrain.superkassa.domain.analytics.model.placement
import kz.mybrain.superkassa.presentation.analytics.map.KkmGroup
import kz.mybrain.superkassa.presentation.analytics.map.MapParts
import kz.mybrain.superkassa.presentation.analytics.map.sieved
import kz.mybrain.superkassa.presentation.common.mapview.MapFold
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons

/**
 * Почему на месте карты стоит объяснение, а не карта.
 *
 * Три случая, и путать их нельзя. Отбор ничего не оставил — снимать
 * плашки. Адреса ещё ищутся — ждать: точки появятся сами, и говорить
 * при этом «поставить на карту нечего» неправда. Ставить действительно
 * нечего — идти в список рядом и чинить причины.
 */
internal fun emptyMapReason(placement: Placement, sieved: Boolean, texts: AnalyticsTexts): ScreenState.Empty = when {
    sieved -> ScreenState.Empty(AppIcons.find, texts.sieve.empty, texts.sieve.emptyHint)
    searchingAll(placement) -> ScreenState.Empty(AppIcons.place, texts.mapSearching, texts.mapSearchingHint)
    else -> ScreenState.Empty(AppIcons.place, texts.mapEmpty, texts.mapEmptyHint)
}

/** Все кассы набора ждут ответа поиска по адресу — и ни одна не отказная. */
private fun searchingAll(placement: Placement): Boolean =
    placement.unplaced.isNotEmpty() && placement.unplaced.all { it.reason == PlacementTrouble.Searching }

/**
 * Что стоит под картой.
 *
 * Выбранная касса — её карточкой; раскрытое место без выбранной кассы —
 * списком своих касс; ничего не выбрано — приглашением выбрать. Порядок
 * именно такой: дойдя до кассы, владелец читает её, а не список места,
 * из которого он в неё пришёл.
 *
 * Пока на карте нет ни одной точки, карточки нет вовсе: приглашение
 * «нажмите точку» стояло под объяснением о том, что точек нет.
 *
 * Карточка сворачивается до заголовка — см. [MapFold]: высоту
 * под ней забирает карта.
 */
@Composable
internal fun UnderMap(parts: MapParts) {
    if (parts.placement.placed.isEmpty()) return
    val spot = parts.groups.firstOrNull { it.id == parts.state.spot }
    val chosen = parts.placement.placed.firstOrNull { it.kkm.cashRegisterId == parts.state.chosen }?.kkm
    if (chosen == null && spot != null && spot.size > 1) {
        SpotUnderMap(parts, spot)
    } else {
        PinUnderMap(parts, chosen, spot?.size ?: 0)
    }
}

/** Раскрытое место с несколькими кассами: их список. */
@Composable
private fun SpotUnderMap(parts: MapParts, spot: KkmGroup) {
    val panel = parts.tools.panel
    AnalyticsSpotCard(
        group = spot,
        texts = parts.texts,
        cabinet = parts.cabinetTexts,
        onChoose = { row -> parts.actions.pick(row.kkm.cashRegisterId) },
        expanded = panel.expanded,
        onToggle = panel::toggle
    )
}

/**
 * Карточка выбранной кассы или подсказка выбрать её.
 *
 * Высота карточки не задана: её содержимое разное, и при заданной
 * строка «Последняя связь» уходила за нижний край.
 */
@Composable
private fun PinUnderMap(parts: MapParts, chosen: AnalyticsKkm?, neighbours: Int) {
    val panel = parts.tools.panel
    AnalyticsPinCard(
        kkm = chosen,
        source = parts.state.source,
        texts = parts.texts,
        cabinet = parts.cabinetTexts,
        neighbours = neighbours,
        onNeighbours = { parts.actions.pick(null) },
        onSales = parts.actions::openSales,
        expanded = panel.expanded,
        onToggle = panel::toggle
    )
}
