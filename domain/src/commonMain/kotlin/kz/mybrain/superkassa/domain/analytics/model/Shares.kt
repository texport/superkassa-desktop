package kz.mybrain.superkassa.domain.analytics.model

import kotlin.math.abs

/**
 * Арифметика сводки в тиынах: суммы, доли и изменения считаются точно,
 * целыми тиынами, как и у ядра кассы.
 */

/** Незаполненная сумма считается нулём, а не прочерком в арифметике. */
fun Long?.orZero(): Long = this ?: 0L

/**
 * Частное с округлением половины от нуля — так же округляет ядро.
 *
 * Делитель положителен: делят на число чеков и на сумму, а не на знак.
 */
internal fun halfUpDiv(dividend: Long, divisor: Long): Long {
    require(divisor > 0) { "divisor must be positive" }
    val quotient = (abs(dividend) * 2 + divisor) / (divisor * 2)
    return if (dividend < 0) -quotient else quotient
}

/** Доля целыми процентами: дробные проценты в таких числах не читают. */
fun percentOf(part: Long, whole: Long): Int = halfUpDiv(part * HUNDRED, whole).toInt()

/** Целое в процентах. */
private const val HUNDRED = 100L
