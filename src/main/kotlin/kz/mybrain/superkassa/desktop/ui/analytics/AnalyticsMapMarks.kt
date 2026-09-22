package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.map.MapMark
import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import kz.mybrain.superkassa.desktop.ui.map.MapState
import kz.mybrain.superkassa.desktop.ui.map.inside

/**
 * Места, попадающие в окно карты.
 *
 * Кабинет отдаёт тысячи касс, а в окно их попадает десяток: ярлычок
 * за краем всё равно обрезан показом, но стоит он полной ценой —
 * своей поверхности, своей тени и своего нажатия. Отбор идёт до того,
 * как сложится хоть один ярлычок, и с ним число ярлычков на кадре
 * зависит от размера окна, а не от размера сети.
 */
internal fun onScreen(groups: List<KkmGroup>, state: MapState, canvas: IntSize): List<KkmGroup> {
    if (canvas.width == 0 || canvas.height == 0) return emptyList()
    val corner = MapProjection.corner(
        state.centerLatitude,
        state.centerLongitude,
        state.zoom,
        canvas.width,
        canvas.height
    )
    return groups.filter { group ->
        inside(MapProjection.screen(group.latitude, group.longitude, state.zoom, corner), canvas)
    }
}

/**
 * Ярлычки по местам.
 *
 * Цвета состояний снимаются со схемы один раз на кадр, а не у каждого
 * ярлычка: раньше место превращалось в ярлычок внутри композиции, и сеть
 * из двух тысяч касс стоила двух тысяч вызовов состава ради трёх цветов,
 * известных заранее.
 */
@Composable
internal fun kkmMarks(groups: List<KkmGroup>, model: AnalyticsMapModel): List<MapMark> {
    val tones = StatusTone.entries.associateWith { toneColor(it) }
    return groups.map { it.mark(tones, model) }
}

/**
 * Место — ярлычком на карте.
 *
 * Число стоит только там, где касс больше одной: единица на ярлычке
 * не сообщает ничего, а превращает карту в поле единиц. Выбранным
 * ярлычок считается и когда раскрыто само место, и когда выбрана одна
 * из его касс: владелец должен видеть, откуда взялась карточка под картой.
 */
private fun KkmGroup.mark(tones: Map<StatusTone, Color>, model: AnalyticsMapModel): MapMark = MapMark(
    id = id,
    latitude = latitude,
    longitude = longitude,
    count = size,
    tone = tones.getValue(groupTone(this)),
    chosen = id == model.spot || holds(model.chosen)
)
