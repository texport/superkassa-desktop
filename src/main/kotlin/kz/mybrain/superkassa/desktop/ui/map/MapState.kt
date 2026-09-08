package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Где сейчас карта и куда поставлена точка.
 *
 * Центр держится в градусах, а не в точках полотна: увеличение меняется,
 * а место остаётся тем же — при пересчёте через точки карта уезжала бы
 * на каждом приближении.
 *
 * @param latitude широта центра при открытии.
 * @param longitude долгота центра при открытии.
 */
class MapState(latitude: Double = ALMATY_LATITUDE, longitude: Double = ALMATY_LONGITUDE, zoom: Int = CITY_ZOOM) {

    var centerLatitude: Double by mutableDoubleStateOf(latitude)
        private set

    var centerLongitude: Double by mutableDoubleStateOf(longitude)
        private set

    var zoom: Int by mutableIntStateOf(zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    var markerLatitude: Double? by mutableStateOf(null)
        private set

    var markerLongitude: Double? by mutableStateOf(null)
        private set

    var locationLatitude: Double? by mutableStateOf(null)
        private set

    var locationLongitude: Double? by mutableStateOf(null)
        private set

    /** Город, в котором нас определили: показывается подписью под картой. */
    var locationCity: String by mutableStateOf("")
        private set

    /**
     * Точное ли это место.
     *
     * Служба геопозиции самой машины указывает на дом, определение
     * по адресу подключения — на город поставщика связи. Разница
     * в километрах, и владелец должен знать, что перед ним.
     */
    var locationPrecise: Boolean by mutableStateOf(false)
        private set

    /** Выбрана ли точка. */
    val marked: Boolean get() = markerLatitude != null && markerLongitude != null

    /** Сдвигает карту на столько точек, на сколько владелец потянул. */
    fun pan(dx: Float, dy: Float) {
        val x = MapProjection.xOf(centerLongitude, zoom) - dx
        val y = MapProjection.yOf(centerLatitude, zoom) - dy
        centerLongitude = MapProjection.longitudeOf(x, zoom)
        centerLatitude = MapProjection.latitudeOf(y.coerceIn(0.0, MapProjection.world(zoom)), zoom)
    }

    /** Приближает или отдаляет, оставляя центр на месте. */
    fun zoomBy(steps: Int) {
        zoom = (zoom + steps).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /** Ставит точку. Центр не двигается: карта под рукой владельца не должна прыгать. */
    fun mark(latitude: Double, longitude: Double) {
        markerLatitude = latitude.coerceIn(-MapProjection.MAX_LATITUDE, MapProjection.MAX_LATITUDE)
        markerLongitude = longitude.coerceIn(-HALF_TURN, HALF_TURN)
    }

    /**
     * Показывает уже известную точку: и метка, и центр.
     *
     * Нужно при открытии карты у точки, которую уже переносили: иначе
     * владелец видит середину страны вместо своего магазина.
     */
    fun show(latitude: Double, longitude: Double, toZoom: Int? = null) {
        mark(latitude, longitude)
        centerLatitude = markerLatitude ?: latitude
        centerLongitude = markerLongitude ?: longitude
        toZoom?.let { zoom = it.coerceIn(MIN_ZOOM, MAX_ZOOM) }
    }

    /**
     * Отмечает, где мы, и ведёт туда карту.
     *
     * Своё место — не выбранная точка: оно определено до города и рисуется
     * своим знаком, синим кружком, а выбранная точка остаётся красной
     * булавкой. Прежде кнопка только двигала карту, и владелец не видел,
     * произошло ли хоть что-нибудь.
     */
    fun showLocation(latitude: Double, longitude: Double, city: String, toZoom: Int, precise: Boolean = false) {
        locationLatitude = latitude.coerceIn(-MapProjection.MAX_LATITUDE, MapProjection.MAX_LATITUDE)
        locationLongitude = longitude.coerceIn(-HALF_TURN, HALF_TURN)
        locationCity = city
        locationPrecise = precise
        centerLatitude = locationLatitude ?: latitude
        centerLongitude = locationLongitude ?: longitude
        zoom = toZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /** Знаем ли, где мы. */
    val located: Boolean get() = locationLatitude != null && locationLongitude != null

    private companion object {
        /** Середина Алматы: с чего-то карта начинаться должна. */
        const val ALMATY_LATITUDE = 43.238949
        const val ALMATY_LONGITUDE = 76.889709

        /** Увеличение, на котором виден город целиком. */
        const val CITY_ZOOM = 12

        const val MIN_ZOOM = 3
        const val MAX_ZOOM = 18
        const val HALF_TURN = 180.0
    }
}
