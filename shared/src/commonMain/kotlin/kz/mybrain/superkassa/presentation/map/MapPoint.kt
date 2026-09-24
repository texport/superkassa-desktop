package kz.mybrain.superkassa.presentation.map

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kotlin.math.abs
import kotlin.math.roundToLong

/** Где стоит торговая точка: широта и долгота, как их ждёт кабинет. */
data class MapPoint(val latitude: Decimal, val longitude: Decimal)

/**
 * Градусы в том виде, в каком их принимает кабинет.
 *
 * Шесть знаков после запятой — примерно десятая доля метра: точнее
 * торговую точку не ставят, а лишние знаки в заявлении выглядят
 * ложной точностью. Нули в конце не пишутся.
 */
internal fun cabinetDegrees(value: Double): Decimal {
    var unscaled = (value * DEGREE_UNIT).roundToLong()
    var scale = DEGREE_SCALE
    while (scale > 0 && unscaled % DECIMAL_BASE == 0L) {
        unscaled /= DECIMAL_BASE
        scale--
    }
    return Decimal(unscaled, scale)
}

/** Градусы для карты: карта считает в числах с плавающей точкой, кабинет — точно. */
fun Decimal.degrees(): Double {
    var divisor = 1.0
    repeat(scale) { divisor *= DECIMAL_BASE }
    return unscaled / divisor
}

/** Пределы широты и долготы в градусах — те же, что у кабинета. */
const val MAX_LATITUDE = "90"
const val MAX_LONGITUDE = "180"

/**
 * Градусы из набранного, если это число в допустимых пределах; иначе `null`.
 *
 * Пределы те же, что у кабинета: он отвергает выходящее за них, и узнать
 * об этом после нажатия — значит потерять весь заполненный ввод. Запятая
 * принимается наравне с точкой: так градусы набирают на русской
 * и казахской раскладке.
 */
fun degreesOf(value: String, limit: String): Decimal? {
    val typed = runCatching { Decimal.parse(value.trim().replace(',', '.')) }.getOrNull() ?: return null
    return typed.takeIf { Decimal(abs(it.unscaled), it.scale) <= Decimal.parse(limit) }
}

private const val DEGREE_SCALE = 6
private const val DEGREE_UNIT = 1_000_000.0
private const val DECIMAL_BASE = 10L
