package kz.mybrain.superkassa.desktop.ui.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

/**
 * Геометрия ряда столбиков.
 *
 * Отделена от полотна: высота столбика, столбик под указателем и частота
 * подписей оси — свойства ряда, а не рисования, и проверяются они счётом,
 * без единой отрисовки.
 */

/** Какой столбик стоит под этой точкой; за пределами полотна — никакой. */
internal fun barAt(x: Float, width: Int, count: Int): Int? {
    if (count <= 0 || width <= 0) return null
    val inside = x >= 0f && x <= width
    if (!inside) return null
    return (x / width * count).toInt().coerceIn(0, count - 1)
}

/** Какую долю высоты занимает столбик: ноль и отрицательное не рисуются. */
internal fun barShare(value: BigDecimal, top: BigDecimal): Float {
    if (top.signum() <= 0 || value.signum() <= 0) return 0f
    return value.divide(top, SHARE_SCALE, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
}

/** Через сколько столбиков подписывать ось. */
internal fun axisStep(count: Int): Int =
    maxOf(1, ceil(count / AXIS_LABELS.toDouble()).toInt())

/** Сколько подписей помещается под графиком, не слипаясь. */
private const val AXIS_LABELS = 8

/** Знаков в доле столбика: полотно всё равно меряет в точках. */
private const val SHARE_SCALE = 4
