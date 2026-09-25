package kz.mybrain.superkassa.presentation.cabinet.places

/**
 * Порядок названий, каким его ждёт владелец: по казахскому алфавиту
 * и с числами как числами.
 *
 * Сравнение строк по кодам знаков ставило казахские буквы после «я»:
 * «Қаракөз» оказывалась в конце списка из двух тысяч точек, а не между
 * «К» и «Л». Числа сравнивались знак за знаком, и «Магазин 10» стоял
 * перед «Магазин 2». Русский алфавит — часть казахского, поэтому один
 * порядок годится для обоих языков.
 */
internal object NameOrder : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val numbered = a[i].isDigit() && b[j].isDigit()
            val nextA = if (numbered) digitsEnd(a, i) else i + 1
            val nextB = if (numbered) digitsEnd(b, j) else j + 1
            val step = if (numbered) {
                numbers(a.substring(i, nextA), b.substring(j, nextB))
            } else {
                rank(a[i]).compareTo(rank(b[j]))
            }
            if (step != 0) return step
            i = nextA
            j = nextB
        }
        return (a.length - i).compareTo(b.length - j)
    }

    private fun digitsEnd(text: String, from: Int): Int {
        var end = from
        while (end < text.length && text[end].isDigit()) end++
        return end
    }

    /** Числа без ведущих нулей: короче — меньше, равной длины — по знакам. */
    private fun numbers(a: String, b: String): Int {
        val x = a.trimStart('0')
        val y = b.trimStart('0')
        return if (x.length != y.length) x.length.compareTo(y.length) else x.compareTo(y)
    }

    /** Место знака: знаки и цифры, затем буквы алфавита, затем прочие буквы. */
    private fun rank(char: Char): Int {
        val lower = char.lowercaseChar()
        val letter = ALPHABET.indexOf(lower)
        return when {
            letter >= 0 -> ALPHABET_FROM + letter
            lower.isLetter() -> OTHER_LETTERS_FROM + lower.code
            else -> lower.code
        }
    }

    /** Казахский алфавит: в нём и все буквы русского. */
    private const val ALPHABET = "аәбвгғдеёжзийкқлмнңоөпрстуұүфхһцчшщъыіьэюя"

    /** Буквы алфавита — после знаков и цифр. */
    private const val ALPHABET_FROM = 0x10000

    /** Прочие буквы — после алфавита. */
    private const val OTHER_LETTERS_FROM = 0x20000
}
