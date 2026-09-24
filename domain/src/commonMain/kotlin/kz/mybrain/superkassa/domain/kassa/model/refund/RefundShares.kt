package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import kz.mybrain.superkassa.domain.kassa.model.Tenge

/**
 * Сколько вернуть за каждую строку чека-основания, в тиынах.
 *
 * Строка чека стоит своей суммы, а чек — своего итога, и они расходятся,
 * когда на весь чек дана скидка или наценка: три строки по тысяче при
 * итоге в девятьсот. Прежде отметка строки возвращала её тысячу, и все
 * отметки вместе давали больше, чем покупатель заплатил. Итог чека
 * делится по строкам пропорционально их суммам; остаток деления
 * до тиына отдаётся строкам с наибольшей дробью — отмеченные все вместе
 * дают ровно итог чека.
 *
 * Сторнированная строка в чеке не продана: её доля — ноль.
 */
internal fun refundShares(items: List<ReceiptItemView>, basisTiyn: Long): List<Long> {
    val weights = items.map { if (it.isStorno) 0L else Tenge.truncated(it.sum) }
    val whole = weights.sum()
    if (whole <= 0L || whole == basisTiyn || basisTiyn < 0L) return weights
    val parts = weights.map { divide(it.toULong(), basisTiyn.toULong(), whole.toULong()) }
    val shares = parts.map { it.first.toLong() }.toMutableList()
    val left = basisTiyn - shares.sum()
    parts.indices.sortedByDescending { parts[it].second }
        .filter { weights[it] > 0L }
        .take(left.toInt().coerceAtLeast(0))
        .forEach { shares[it] += 1L }
    return shares
}

/**
 * Частное и остаток `a × b / d` без переполнения.
 *
 * Доля строки — её сумма на итог чека, делённые на сумму строк: у чека
 * в десятки миллионов тенге произведение не помещается в целое число,
 * а доля обязана быть точной до тиына.
 */
private fun divide(a: ULong, b: ULong, d: ULong): Pair<ULong, ULong> {
    val (high, low) = product(a, b)
    var quotient = 0UL
    var rest = 0UL
    for (bit in WIDE_BITS - 1 downTo 0) {
        val next = if (bit >= WORD_BITS) (high shr (bit - WORD_BITS)) and 1UL else (low shr bit) and 1UL
        rest = (rest shl 1) or next
        if (rest >= d) {
            rest -= d
            if (bit < WORD_BITS) quotient = quotient or (1UL shl bit)
        }
    }
    return quotient to rest
}

/** Произведение двух целых в двух словах: старшее и младшее. */
private fun product(a: ULong, b: ULong): Pair<ULong, ULong> {
    val low = (a and HALF_MASK) * (b and HALF_MASK)
    val cross1 = (a shr HALF_BITS) * (b and HALF_MASK)
    val cross2 = (a and HALF_MASK) * (b shr HALF_BITS)
    val middle = (low shr HALF_BITS) + (cross1 and HALF_MASK) + (cross2 and HALF_MASK)
    val high = (a shr HALF_BITS) * (b shr HALF_BITS) + (cross1 shr HALF_BITS) + (cross2 shr HALF_BITS) +
        (middle shr HALF_BITS)
    return high to ((low and HALF_MASK) or (middle shl HALF_BITS))
}

private const val WORD_BITS = 64
private const val WIDE_BITS = 128
private const val HALF_BITS = 32
private const val HALF_MASK = 0xFFFF_FFFFUL
