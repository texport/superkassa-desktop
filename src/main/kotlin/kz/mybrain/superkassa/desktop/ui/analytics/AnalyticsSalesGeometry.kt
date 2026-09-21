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

/**
 * Сколько делений отводится ряду из такого числа столбиков.
 *
 * Не меньше недели. Столбик занимает своё деление целиком, и у ряда
 * из одного дня деление было во всю ширину карточки: сплошная заливка
 * от края до края читается не как столбик, а как поломка разметки.
 * Неделя делений оставляет одному дню столбик привычной ширины, а пустые
 * деления рядом пустыми и остаются — торговали-то один день.
 */
internal fun chartSlots(count: Int): Int = maxOf(count, MIN_SLOTS)

/** Какой столбик стоит под этой точкой; за пределами ряда — никакой. */
internal fun barAt(x: Float, width: Int, count: Int): Int? {
    if (count <= 0 || width <= 0) return null
    val inside = x >= 0f && x <= width
    if (!inside) return null
    // Делений бывает больше, чем столбиков: точка над пустым делением
    // не называет последний столбик, а не называет никакого.
    val slots = chartSlots(count)
    val slot = (x / width * slots).toInt().coerceAtMost(slots - 1)
    return slot.takeIf { it in 0 until count }
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

/** Наименьшее число делений у ряда: неделя. */
private const val MIN_SLOTS = 7

/** Знаков в доле столбика: полотно всё равно меряет в точках. */
private const val SHARE_SCALE = 4
