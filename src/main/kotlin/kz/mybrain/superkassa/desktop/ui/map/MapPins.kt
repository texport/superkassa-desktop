package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kotlin.math.hypot

/**
 * Одна из многих точек на карте.
 *
 * Выбранная точка карты — своя, её ставит владелец нажатием; эти же
 * приходят готовым набором: кассы компании, каждая на своём месте.
 * Поэтому у знака есть имя — по нему показ узнаёт, по какой кассе
 * пришлось нажатие, и какую из них подсветить.
 */
data class MapPin(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val chosen: Boolean = false
)

/** Цвета знаков: выбранный, остальные и обводка. */
data class MapPinPaint(val chosen: Color, val other: Color, val edge: Color)

/**
 * По какому знаку пришлось нажатие.
 *
 * Промах — `null`: на карте с сотней касс попасть точно в кружок нельзя,
 * поэтому знак ловит нажатие в пределах [reach] вокруг себя, а из
 * нескольких попавших выигрывает ближний. Выбранный знак крупнее и ловит
 * дальше — но только он, иначе соседи перекрывали бы друг друга.
 */
fun pinAt(pins: List<MapPin>, at: MapPixel, zoom: Int, corner: MapPixel, reach: Double): MapPin? =
    pins
        .map { pin -> pin to distance(pin, at, zoom, corner) }
        .filter { (_, away) -> away <= reach }
        .minByOrNull { (_, away) -> away }
        ?.first

/** Сколько точек полотна между знаком и нажатием. */
private fun distance(pin: MapPin, at: MapPixel, zoom: Int, corner: MapPixel): Double {
    val place = MapProjection.screen(pin.latitude, pin.longitude, zoom, corner)
    return hypot(place.x - at.x, place.y - at.y)
}

/**
 * Рисует все знаки разом.
 *
 * Выбранный рисуется последним: на плотной карте знаки наезжают друг
 * на друга, и тот, о котором сейчас читают карточку, обязан быть сверху.
 */
internal fun DrawScope.drawPins(pins: List<MapPin>, zoom: Int, corner: MapPixel, paint: MapPinPaint) {
    pins.sortedBy { it.chosen }.forEach { pin ->
        val place = MapProjection.screen(pin.latitude, pin.longitude, zoom, corner)
        val at = Offset(place.x.toFloat(), place.y.toFloat())
        val radius = if (pin.chosen) Sizes.mapPinChosen.toPx() else Sizes.mapPin.toPx()
        drawCircle(paint.edge, radius = radius + Sizes.mapMarkerEdge.toPx(), center = at)
        drawCircle(if (pin.chosen) paint.chosen else paint.other, radius = radius, center = at)
    }
}

/**
 * Нажатие по карте.
 *
 * Карта живёт в приложении двумя жизнями. В окне выбора места торговой
 * точки нажатие ставит точку — это и есть весь смысл того окна. На карте
 * касс ставить нечего: там нажатием выбирают кассу, а промах снимает
 * выбор. Различает их обработчик знаков: задан — карта касс, нет —
 * прежний выбор точки.
 */
internal fun MapState.tapped(
    canvas: IntSize,
    at: Offset,
    pins: List<MapPin>,
    onPin: ((MapPin?) -> Unit)?,
    reach: Float
) {
    val corner = MapProjection.corner(centerLatitude, centerLongitude, zoom, canvas.width, canvas.height)
    if (onPin != null) {
        onPin(pinAt(pins, MapPixel(at.x.toDouble(), at.y.toDouble()), zoom, corner, reach.toDouble()))
        return
    }
    mark(
        MapProjection.latitudeOf(corner.y + at.y, zoom),
        MapProjection.longitudeOf(corner.x + at.x, zoom)
    )
}
