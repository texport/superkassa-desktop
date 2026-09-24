package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kotlin.math.abs

/**
 * Доли в процентах от сумм в тиынах.
 *
 * Округление одно на скидку и наценку — на чек и на позицию: до тиына,
 * к ближайшему, половина от нуля. Тем же правилом считает касса, и
 * расхождение в один тиын — это расхождение с БФД, а не мелочь на экране.
 */
object Percent {

    /** [percent] процентов от [base], в тиынах. */
    fun of(base: Long, percent: Decimal): Long =
        halfUp(times(base, percent.unscaled), HUNDRED * pow10(percent.scale))

    /**
     * Какую долю [base] составляет [part] — в процентах до сотой.
     *
     * У пустого чека доли нет: делить не на что.
     */
    fun share(base: Long, part: Long): Decimal? =
        if (base <= 0L) null else Decimal.ofScaled(halfUp(times(part, HUNDRED * HUNDRED), base), SHARE_SCALE)

    /**
     * Произведение, упёртое в предел целого вместо переполнения.
     *
     * Такие числа — опечатка, а не сумма, и правила их отвергают; но знак
     * и порядок они обязаны сохранить: переполненная скидка становилась
     * отрицательной и проходила мимо проверки «больше суммы позиций».
     */
    private fun times(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        val product = a * b
        val overflow = product / b != a || (a == Long.MIN_VALUE && b == -1L)
        return if (!overflow) product else if ((a < 0) == (b < 0)) Long.MAX_VALUE else Long.MIN_VALUE
    }

    /** Деление к ближайшему, половина — от нуля; делитель положителен. */
    private fun halfUp(value: Long, divisor: Long): Long {
        val quotient = value / divisor
        val left = abs(value % divisor)
        val away = if (value < 0) -1L else 1L
        return if (left != 0L && left >= divisor - left) quotient + away else quotient
    }

    private fun pow10(power: Int): Long {
        var result = 1L
        repeat(power) { result *= DECIMAL_BASE }
        return result
    }

    private const val HUNDRED = 100L
    private const val DECIMAL_BASE = 10L

    /** Доля показывается до сотой процента: мельче кассир её не набирает. */
    private const val SHARE_SCALE = 2
}
