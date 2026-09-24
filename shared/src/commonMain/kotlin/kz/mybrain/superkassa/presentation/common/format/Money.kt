package kz.mybrain.superkassa.presentation.common.format

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kotlin.math.abs
import io.github.texport.superkassa.core.domain.api.model.common.Money as KassaMoney

/**
 * Деньги на экране.
 *
 * Валюта — тенге, сотая доля называется тиын. Сумма живёт целым числом
 * тиынов, как у ядра кассы: сложение и сравнение точны на любой платформе,
 * а двоичной дроби посередине нет. Округление банковское не применяется:
 * касса показывает ровно то, что отправила в ОФД, поэтому доля тиына
 * в показе отбрасывается.
 *
 * Разряды разделяет неразрывный пробел из общего набора знаков [Glyphs]:
 * в исходнике он неотличим от обычного, и однажды он уже развёл показ
 * и разбор — сумма «1 200» с обычным пробелом не принималась.
 */
object Money {

    /**
     * Сумма в тиынах словами экрана: «1 234,56 ₸»; незаполненная — прочерк.
     *
     * Знак вычитания и дефис переноса на экране разной ширины, и столбец
     * сумм, где сторно набрано дефисом, а возврат — минусом, стоит рваным.
     * Знак ставится у всей суммы, а не у целой части: у «−0,50» целых
     * нулей, и минус пропадал вместе с ними — сторно на полтиына
     * выглядело обычной продажей.
     */
    fun formatTiyn(amount: Long?): String {
        val tiyn = amount ?: return Glyphs.DASH
        val sign = if (tiyn < 0) Glyphs.MINUS else ""
        return "$sign${grouped(plain(tiyn))}${Glyphs.NBSP}${Glyphs.TENGE}"
    }

    /**
     * Сумма десятичным числом ядра: так её отдают кабинет и чек ядра.
     *
     * Лишние знаки после тиына отбрасываются, как и у любой показанной суммы.
     */
    fun format(amount: Decimal?): String = formatTiyn(amount?.let(Tenge::truncated))

    /**
     * Счёт штук — теми же разрядами, что и деньги, но без валюты.
     *
     * Чеков и касс бывает пять цифр, и рядом с «128 456 000,00 ₸» число
     * «12845» читалось как другой порядок величины. Правило разбивки одно
     * на деньги и на счёт: своя копия в аналитике разошлась бы с этой
     * на первой правке.
     */
    fun count(value: Number): String {
        val number = value.toLong()
        val sign = if (number < 0) "-" else ""
        return sign + groupThousands(abs(number).toString())
    }

    /**
     * Количество без незначащих нулей: «2», «0.5»; незаполненное — прочерк.
     *
     * Кабинет отдаёт количество с тремя знаками после запятой, и
     * «2,000 × 450,00 ₸» читается как две тысячи штук.
     */
    fun quantity(value: Decimal?): String = value?.toString()
        ?.let { if ('.' in it) it.trimEnd('0').trimEnd('.') else it }
        ?: Glyphs.DASH

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

    /** Сумма без знака и разрядов: «1234,56». */
    private fun plain(tiyn: Long): String {
        val part = abs(tiyn % KassaMoney.TIYN_IN_TENGE).toString().padStart(Tenge.TIYN_SCALE, '0')
        return "${abs(tiyn / KassaMoney.TIYN_IN_TENGE)}${Glyphs.DECIMAL}$part"
    }

    /** Разряды разделяются неразрывным пробелом, чтобы сумма не рвалась переносом. */
    private fun groupThousands(digits: String): String =
        digits.reversed()
            .chunked(GROUP_SIZE)
            .joinToString(Glyphs.NBSP.toString())
            .reversed()

    private const val GROUP_SIZE = 3
}
