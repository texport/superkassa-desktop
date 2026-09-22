package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.map.MapMark
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Место — ярлычком на карте.
 *
 * Число стоит только там, где касс больше одной: единица на ярлычке
 * не сообщает ничего, а превращает карту в поле единиц.
 */
@Composable
internal fun KkmGroup.mark(model: AnalyticsMapModel): MapMark = MapMark(
    id = id,
    latitude = latitude,
    longitude = longitude,
    label = size.takeIf { it > 1 }?.toString(),
    tone = toneColor(groupTone(this)),
    chosen = id == model.spot || holds(model.chosen)
)

/**
 * Почему на месте карты стоит объяснение, а не карта.
 *
 * Три случая, и путать их нельзя. Отбор ничего не оставил — снимать
 * плашки. Адреса ещё ищутся — ждать: точки появятся сами, и говорить
 * при этом «поставить на карту нечего» неправда. Ставить действительно
 * нечего — идти в список рядом и чинить причины.
 */
internal fun emptyMapReason(placement: Placement, sieved: Boolean, texts: AnalyticsTexts): ScreenState.Empty = when {
    sieved -> ScreenState.Empty(AppIcons.find, texts.sieveEmpty, texts.sieveEmptyHint)
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
 * Карточка сворачивается до заголовка — см. [AnalyticsMapCard]: высоту
 * под ней забирает карта.
 */
@Composable
internal fun UnderMap(
    model: AnalyticsMapModel,
    placement: Placement,
    groups: List<KkmGroup>,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    panel: AnalyticsMapCard
) {
    if (placement.placed.isEmpty()) return
    val spot = groups.firstOrNull { it.id == model.spot }
    val chosen = placement.placed.firstOrNull { it.kkm.cashRegisterId == model.chosen }?.kkm
    if (chosen == null && spot != null && spot.size > 1) {
        AnalyticsSpotCard(
            group = spot,
            texts = texts,
            cabinet = cabinetTexts,
            onChoose = { row -> model.chosen = row.kkm.cashRegisterId },
            expanded = panel.expanded,
            onToggle = panel::toggle
        )
        return
    }
    // Высота карточки не задана: её содержимое разное, и при заданной
    // строка «Последняя связь» уходила за нижний край.
    AnalyticsPinCard(
        kkm = chosen,
        source = model.source,
        texts = texts,
        cabinet = cabinetTexts,
        neighbours = spot?.size ?: 0,
        onNeighbours = { model.chosen = null },
        onSales = { model.opened = it },
        expanded = panel.expanded,
        onToggle = panel::toggle
    )
}
