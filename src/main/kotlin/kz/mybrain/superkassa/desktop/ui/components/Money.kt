package kz.mybrain.superkassa.desktop.ui.components

import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Деньги на экране.
 *
 * Валюта — тенге, сотая доля называется тиын. Округление банковское не
 * применяется: касса показывает ровно то, что отправила в ОФД, поэтому
 * усечение идёт вниз до тиына.
 *
 * Разряды разделяет неразрывный пробел из общего набора знаков [Glyphs]:
 * в исходнике он неотличим от обычного, и однажды он уже развёл показ
 * и разбор — сумма «1 200» с обычным пробелом не принималась.
 */
object Money {

    /**
     * Тиын — сотая доля тенге, и глубже деньги не делятся.
     *
     * Один на всё приложение: сумму в тиынах считают касса, возврат,
     * денежный ящик и разбор введённого, и разойтись этому числу нельзя.
     */
    const val TIYN_SCALE: Int = 2

    private const val CURRENCY = "₸"
    private const val GROUP_SIZE = 3

    /**
     * Сумма из журнала кассы.
     *
     * Сервер отдаёт деньги в тиынах целым числом: тенге с дробной частью
     * в JSON пришлось бы округлять, а чек округления не прощает.
     */
    fun formatTiyn(amount: Long?): String {
        val value = amount ?: return Glyphs.DASH
        return format(BigDecimal.valueOf(value, TIYN_SCALE))
    }

    /**
     * Тиыны из журнала — в тенге для запроса к серверу.
     *
     * Обратный возврат оформляется от суммы настоящего чека, а чек хранится
     * в тиынах: без деления в запрос уходила сумма в сто раз больше.
     */
    fun tengeOf(tiyn: Long): BigDecimal = BigDecimal.valueOf(tiyn, TIYN_SCALE)

    fun format(amount: BigDecimal): String {
        val scaled = amount.setScale(TIYN_SCALE, RoundingMode.DOWN)
        // Знак берётся у всей суммы, а не у целой части: у «−0,50» целых
        // нулей, и минус пропадал вместе с ними — сторно на полтиына
        // выглядело обычной продажей.
        val negative = scaled.signum() < 0
        val whole = scaled.abs().toBigInteger().toString()
        val fraction = scaled.remainder(BigDecimal.ONE).abs().movePointRight(TIYN_SCALE).toBigInteger()
        val tiyn = fraction.toString().padStart(TIYN_SCALE, '0')
        val sign = if (negative) "-" else ""
        return "$sign${groupThousands(whole)},$tiyn${Glyphs.NBSP}$CURRENCY"
    }

    /** Разряды разделяются неразрывным пробелом, чтобы сумма не рвалась переносом. */
    private fun groupThousands(digits: String): String =
        digits.reversed()
            .chunked(GROUP_SIZE)
            .joinToString(Glyphs.NBSP.toString())
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
            .filterNot { it == ' ' || it == Glyphs.NBSP }
            .trim()
        if (normalized.isEmpty()) return null
        return normalized.toBigDecimalOrNull()?.takeIf { it.scale() <= TIYN_SCALE }
    }
}
