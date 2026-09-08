package kz.mybrain.superkassa.desktop.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Деньги на экране.
 *
 * Валюта — тенге, сотая доля называется тиын. Округление банковское не
 * применяется: касса показывает ровно то, что отправила в ОФД, поэтому
 * усечение идёт вниз до тиына.
 *
 * Разделители заданы escape-последовательностями намеренно: неразрывный
 * пробел в исходнике неотличим от обычного, и однажды он уже развёл показ
 * и разбор — сумма «1 200» с обычным пробелом не принималась.
 */
object Money {
    private const val CURRENCY = "₸"
    private const val GROUP_SEPARATOR = '\u00A0'
    private const val DECIMALS = 2
    private const val GROUP_SIZE = 3

    /**
     * Сумма из журнала кассы.
     *
     * Сервер отдаёт деньги в тиынах целым числом: тенге с дробной частью
     * в JSON пришлось бы округлять, а чек округления не прощает.
     */
    fun formatTiyn(amount: Long?): String {
        val value = amount ?: return "—"
        return format(BigDecimal.valueOf(value, DECIMALS))
    }

    /**
     * Тиыны из журнала — в тенге для запроса к серверу.
     *
     * Обратный возврат оформляется от суммы настоящего чека, а чек хранится
     * в тиынах: без деления в запрос уходила сумма в сто раз больше.
     */
    fun tengeOf(tiyn: Long): BigDecimal = BigDecimal.valueOf(tiyn, DECIMALS)

    fun format(amount: BigDecimal): String {
        val scaled = amount.setScale(DECIMALS, RoundingMode.DOWN)
        // Знак берётся у всей суммы, а не у целой части: у «−0,50» целых
        // нулей, и минус пропадал вместе с ними — сторно на полтиына
        // выглядело обычной продажей.
        val negative = scaled.signum() < 0
        val whole = scaled.abs().toBigInteger().toString()
        val fraction = scaled.remainder(BigDecimal.ONE).abs().movePointRight(DECIMALS).toBigInteger()
        val tiyn = fraction.toString().padStart(DECIMALS, '0')
        val sign = if (negative) "-" else ""
        return "$sign${groupThousands(whole)},$tiyn${GROUP_SEPARATOR}$CURRENCY"
    }

    /** Разряды разделяются неразрывным пробелом, чтобы сумма не рвалась переносом. */
    private fun groupThousands(digits: String): String =
        digits.reversed()
            .chunked(GROUP_SIZE)
            .joinToString(GROUP_SEPARATOR.toString())
            .reversed()

    /**
     * Разбор суммы, введённой кассиром.
     *
     * Принимаются и запятая, и точка, и оба вида пробела: кассир может
     * вставить сумму из отчёта, где разряды разделены неразрывным пробелом.
     */
    fun parse(text: String): BigDecimal? {
        val normalized = text
            .replace(',', '.')
            .filterNot { it == ' ' || it == GROUP_SEPARATOR }
            .trim()
        if (normalized.isEmpty()) return null
        return normalized.toBigDecimalOrNull()?.takeIf { it.scale() <= DECIMALS }
    }
}
