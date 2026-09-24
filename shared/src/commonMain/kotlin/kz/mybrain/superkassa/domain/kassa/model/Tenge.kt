package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kotlin.math.abs
import io.github.texport.superkassa.core.domain.api.model.common.Money as KassaMoney

/**
 * Деньги кассы в тиынах — правилами ядра.
 *
 * Сумма чека, возврата и ящика живёт целым числом тиынов: сложение
 * и сравнение точны на любой платформе, а двоичной дроби посередине нет.
 * Дробь нужна только там, где её требует сам расчёт: цена на количество;
 * доля в процентах — у [Percent]. Своего округления строки здесь нет — его объявляет
 * ядро, и касса, считавшая строку чека иначе, получала отказ несходящейся
 * оплатой.
 */
object Tenge {

    /** Тиын — сотая доля тенге, и глубже деньги не делятся. */
    const val TIYN_SCALE: Int = 2

    /**
     * Стоимость строки чека: цена в тиынах × количество, к ближайшему тиыну.
     *
     * Половина тиына идёт вверх: 333,33 × 1,5 = 499,995 — это 500,00.
     */
    fun lineSum(price: Long, quantity: Decimal): Long = KassaMoney.lineSum(decimal(price), quantity).tiyn()

    /** Тиыны — десятичным числом ядра: в нём касса принимает суммы чека. */
    fun decimal(tiyn: Long): Decimal = Decimal.ofScaled(tiyn, TIYN_SCALE)

    /** Сумма десятичным числом ядра — в тиыны, доля тиына к ближайшему, как у ядра. */
    fun of(amount: Decimal): Long = amount.scaled(TIYN_SCALE)

    /**
     * Сумма десятичным числом ядра — в тиыны с отбрасыванием доли тиына.
     *
     * Для того, что касса уже посчитала: показанное и поделённое не
     * округляется заново.
     */
    fun truncated(amount: Decimal): Long {
        var value = amount.unscaled
        repeat(amount.scale - TIYN_SCALE) { value /= DECIMAL_BASE }
        repeat(TIYN_SCALE - amount.scale) { value *= DECIMAL_BASE }
        return value
    }

    /**
     * Сумма в тиынах в том виде, в каком её набирают в поле: «1234,56».
     *
     * Дробь отделена запятой — тем же знаком, каким её показывают деньги:
     * подставленная касса сумма должна выглядеть так же, как набранная
     * кассиром, а не другой записью того же числа.
     */
    fun entered(tiyn: Long): String {
        val sign = if (tiyn < 0) "-" else ""
        val part = abs(tiyn % KassaMoney.TIYN_IN_TENGE).toString().padStart(TIYN_SCALE, '0')
        return "$sign${abs(tiyn / KassaMoney.TIYN_IN_TENGE)}$DECIMAL_MARK$part"
    }

    /**
     * Сумма, набранная кассиром, — в тиыны.
     *
     * Принимаются и запятая, и точка, и любые пробелы: кассир вставляет сумму
     * из отчёта, где разряды разделены неразрывным пробелом. Больше двух
     * знаков после запятой не принимается: доли тиына касса не выдаст,
     * а молча округлять набранное нельзя. Знак принимается, чтобы отказ
     * назвал сумму отрицательной, а не «не числом».
     *
     * @return тиыны или `null`, если набрано не число или не сумма денег.
     */
    fun parse(text: String): Long? {
        val normalized = text.replace(',', '.').filterNot { it.isWhitespace() || it == NBSP }
        val body = normalized.removePrefix("-").removePrefix("+")
        val whole = body.substringBefore('.')
        val fraction = body.substringAfter('.', "")
        if (!isAmount(whole, fraction)) return null
        val tiyn = (whole.trimStart('0') + fraction.padEnd(TIYN_SCALE, '0')).toLong()
        return if (normalized.startsWith('-')) -tiyn else tiyn
    }

    /** Целая часть и дробь — цифры, дробь не глубже тиына, целое помещается в тиыны. */
    private fun isAmount(whole: String, fraction: String): Boolean {
        val digits = whole + fraction
        val fits = fraction.length <= TIYN_SCALE && whole.trimStart('0').length <= MAX_TENGE_DIGITS
        return fits && digits.isNotEmpty() && digits.all(Char::isDigit)
    }

    private const val DECIMAL_BASE = 10L
    private const val NBSP = '\u00A0'
    private const val DECIMAL_MARK = ','

    /**
     * Цифр в целых тенге, которые помещаются в тиыны без переполнения.
     *
     * Шестнадцать цифр — это на порядки больше любого чека, а семнадцатая
     * уже подходит к пределу целого числа тиынов: такая сумма — не сумма,
     * а опечатка.
     */
    private const val MAX_TENGE_DIGITS = 16
}
