package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.ui.map.MapProjection
import kotlin.math.abs

/** Куда вести карту, чтобы весь набор касс был виден разом. */
data class AnalyticsMapFit(val latitude: Double, val longitude: Double, val zoom: Int)

/**
 * Середина набора точек и увеличение, на котором он помещается в окно.
 *
 * Без этого карта открывалась бы там, где ей велено по умолчанию, —
 * посреди Алматы, — а кассы сети из Уральска оставались бы за краем
 * окна, и владелец видел бы пустую карту при полном списке.
 *
 * Увеличение подбирается от крупного к мелкому: берётся самое крупное,
 * при котором размах набора ещё умещается в окно. Одна касса — самое
 * крупное из возможных: на нём различимы дома.
 */
fun fitting(points: List<PlacedKkm>, width: Int, height: Int): AnalyticsMapFit? {
    if (points.isEmpty()) return null
    val latitudes = points.map { it.latitude }
    val longitudes = points.map { it.longitude }
    val zoom = (HOUSE_ZOOM downTo COUNTRY_ZOOM).firstOrNull { zoom ->
        fits(latitudes, longitudes, zoom, width, height)
    } ?: COUNTRY_ZOOM
    return AnalyticsMapFit(
        latitude = (latitudes.min() + latitudes.max()) / 2,
        longitude = (longitudes.min() + longitudes.max()) / 2,
        zoom = zoom
    )
}

/** Помещается ли размах набора в окно на этом увеличении. */
private fun fits(latitudes: List<Double>, longitudes: List<Double>, zoom: Int, width: Int, height: Int): Boolean {
    val across = abs(MapProjection.xOf(longitudes.max(), zoom) - MapProjection.xOf(longitudes.min(), zoom))
    val down = abs(MapProjection.yOf(latitudes.max(), zoom) - MapProjection.yOf(latitudes.min(), zoom))
    return across <= width && down <= height
}

/** Увеличение, на котором различимы дома: на нём открывается одиночная касса. */
private const val HOUSE_ZOOM = 17

/** Самое мелкое увеличение: на нём видна страна целиком. */
private const val COUNTRY_ZOOM = 4

/**
 * Размер окна карты, по которому подбирается увеличение.
 *
 * Взят числом, а не снят с полотна: увеличение выбирается до первой
 * отрисовки, когда размера окна ещё нет. Числа близки к тому, сколько
 * карте достаётся в разделе, и промах в них стоит одного шага
 * увеличения, а не пустой карты.
 */
const val FIT_WIDTH: Int = 640

const val FIT_HEIGHT: Int = 420
