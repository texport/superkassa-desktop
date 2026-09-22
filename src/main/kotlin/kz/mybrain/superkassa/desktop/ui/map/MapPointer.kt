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

/**
 * Колесо над картой — в шаги увеличения вокруг указателя.
 *
 * Точка под указателем считается той же проекцией, что и нажатие,
 * а после шага остаётся на месте: см. [MapState.zoomAt].
 */
internal fun MapState.wheeled(canvas: IntSize, at: Offset, steps: Int) {
    if (steps == 0) return
    val corner = MapProjection.corner(centerLatitude, centerLongitude, zoom, canvas.width, canvas.height)
    zoomAt(
        latitude = MapProjection.latitudeOf(corner.y + at.y, zoom),
        longitude = MapProjection.longitudeOf(corner.x + at.x, zoom),
        dx = at.x - canvas.width / 2.0,
        dy = at.y - canvas.height / 2.0,
        steps = steps
    )
}

/**
 * Ход колеса, накопленный до целого шага.
 *
 * Мышь отдаёт колесо целыми щелчками, а сенсорная панель — дробными
 * долями десятками в секунду. Увеличение у карты целое, и каждая доля
 * шагом быть не может: панель уносила бы карту с города на страну одним
 * движением. Доли складываются, и шаг делается, когда набрался целый.
 *
 * Ход «от себя» приближает, «к себе» отдаляет — как в картах в браузере,
 * к которым владелец привык.
 */
internal class MapWheel {

    private var turned = 0f

    /** Сколько шагов увеличения даёт очередная порция хода; ноль — ещё копится. */
    fun turn(delta: Float): Int {
        turned += delta
        val whole = turned.toInt()
        turned -= whole
        return -whole
    }
}
