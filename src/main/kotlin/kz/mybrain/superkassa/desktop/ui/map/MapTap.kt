package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * Нажатие по карте — в градусы.
 *
 * Что значит само нажатие, карта не решает: в окне выбора места оно
 * ставит метку торговой точки, на карте касс — снимает выбор. Прежде
 * это различалось внутри показа по тому, задан ли обработчик знаков,
 * и добавить третий смысл было некуда.
 */
internal fun MapState.tapped(canvas: IntSize, at: Offset, onTap: (Double, Double) -> Unit) {
    val corner = MapProjection.corner(centerLatitude, centerLongitude, zoom, canvas.width, canvas.height)
    onTap(
        MapProjection.latitudeOf(corner.y + at.y, zoom),
        MapProjection.longitudeOf(corner.x + at.x, zoom)
    )
}
