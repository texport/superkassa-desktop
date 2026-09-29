package kz.mybrain.superkassa.presentation.analytics.map

import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.kkm.model.CountryBounds
import kz.mybrain.superkassa.presentation.common.mapview.HOUSE_ZOOM
import kz.mybrain.superkassa.presentation.common.mapview.MapProjection
import kotlin.math.abs

/** Куда вести карту, чтобы весь набор касс был виден разом. */
internal data class AnalyticsMapFit(val latitude: Double, val longitude: Double, val zoom: Int)

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
 *
 * Охватываются кассы в пределах страны ([CountryBounds]); точки вне её —
 * ошибки координат — видны, если отдалить карту, но охват не растягивают.
 * Нет ни одной кассы в стране — охватывается весь набор.
 */
internal fun fitting(points: List<PlacedKkm>, width: Int, height: Int): AnalyticsMapFit? {
    val framed = points.filter { CountryBounds.contains(it.latitude, it.longitude) }.ifEmpty { points }
    if (framed.isEmpty()) return null
    val latitudes = framed.map { it.latitude }
    val longitudes = framed.map { it.longitude }
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
internal const val FIT_WIDTH: Int = 640

internal const val FIT_HEIGHT: Int = 420
