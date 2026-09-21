package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.map.MapMark
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

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
 * Что стоит под картой.
 *
 * Выбранная касса — её карточкой; раскрытое место без выбранной кассы —
 * списком своих касс; ничего не выбрано — приглашением выбрать. Порядок
 * именно такой: дойдя до кассы, владелец читает её, а не список места,
 * из которого он в неё пришёл.
 */
@Composable
internal fun UnderMap(
    model: AnalyticsMapModel,
    placement: Placement,
    groups: List<KkmGroup>,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts
) {
    val spot = groups.firstOrNull { it.id == model.spot }
    val chosen = placement.placed.firstOrNull { it.kkm.cashRegisterId == model.chosen }?.kkm
    if (chosen == null && spot != null && spot.size > 1) {
        AnalyticsSpotCard(spot, texts, onChoose = { row -> model.chosen = row.kkm.cashRegisterId })
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
        onSales = { model.opened = it }
    )
}
