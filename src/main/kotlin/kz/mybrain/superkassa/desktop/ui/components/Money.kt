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

    /**
     * Сумма со знаком: минус берётся из общего набора знаков.
     *
     * Знак вычитания и дефис переноса на экране разной ширины, и столбец
     * сумм, где сторно набрано дефисом, а возврат — минусом, стоит рваным.
     * Знак ставится у всей суммы, а не у целой части: у «−0,50» целых
     * нулей, и минус пропадал вместе с ними — сторно на полтиына
     * выглядело обычной продажей.
     */
    fun format(amount: BigDecimal): String {
        val scaled = amount.setScale(TIYN_SCALE, RoundingMode.DOWN)
        val whole = scaled.abs().toBigInteger().toString()
        val fraction = scaled.remainder(BigDecimal.ONE).abs().movePointRight(TIYN_SCALE).toBigInteger()
        val tiyn = fraction.toString().padStart(TIYN_SCALE, '0')
        val sign = if (scaled.signum() < 0) Glyphs.MINUS else ""
        return "$sign${groupThousands(whole)}${Glyphs.DECIMAL}$tiyn${Glyphs.NBSP}${Glyphs.TENGE}"
    }

    /**
     * Счёт штук — теми же разрядами, что и деньги, но без валюты.
     *
     * Чеков и касс бывает пять цифр, и рядом с «128 456 000,00 ₸» число
     * «12845» читалось как другой порядок величины. Правило разбивки одно
     * на деньги и на счёт: своя копия в аналитике разошлась бы с этой
     * на первой правке.
     */
    fun count(value: Number): String = groupThousands(value.toLong().toString())

    /** Разряды разделяются неразрывным пробелом, чтобы сумма не рвалась переносом. */
    private fun groupThousands(digits: String): String =
        digits.reversed()
            .chunked(GROUP_SIZE)
            .joinToString(Glyphs.NBSP.toString())
            .reversed()

    /**
     * Сумма в том виде, в каком её набирают в поле.
     *
     * Дробь отделена запятой — тем же знаком, каким её показывают деньги
     * и количество: подставленная касса сумма должна выглядеть так же,
     * как набранная кассиром, а не другой записью того же числа. Разряды
     * разделяет само поле, и в набранную строку они не попадают.
     */
    fun entered(amount: BigDecimal): String =
        amount.setScale(TIYN_SCALE, RoundingMode.DOWN).toPlainString().replace('.', Glyphs.DECIMAL)

    /**
     * Введённая сумма в том виде, в каком её показывает поле ввода.
     *
     * Разряды разбиваются тем же неразрывным пробелом, каким они разбиты
     * в подписи рядом: в поле стояло «11372,50», а под ним — «13 860,00 ₸»,
     * и кассир сверял два по-разному набранных числа. Разбивается только
     * целая часть, остальное остаётся как набрано: сумма набирается слева
     * направо, и «1 200,» на полпути к «1 200,50» должна оставаться
     * тем, что кассир видит.
     *
     * Набранное не число возвращается как есть: об этом говорит отказ
     * поля, а не молчаливая подмена введённого.
     */
    fun grouped(entered: String): String {
        val whole = entered.takeWhile { it.isDigit() }
        if (whole.isEmpty()) return entered
        return groupThousands(whole) + entered.drop(whole.length)
    }

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
