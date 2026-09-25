package kz.mybrain.superkassa.strings.api

import kz.mybrain.superkassa.strings.lines
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Каждый язык написан своими буквами.
 *
 * Строку, скопированную из соседнего языка, компилятор не заметит: поле
 * заполнено, и проверка на пустоту зелёная. Кассир же читает на
 * английском экране русскую фразу, а на русском — казахскую букву,
 * оставшуюся от правки перевода.
 */
class AlphabetTest {

    @Test
    fun `в английских надписях нет кириллицы`() {
        lines(textsOf(Language.En)).forEach { (name, value) ->
            assertTrue(foreign(value).none { it in CYRILLIC }, "En: кириллица в надписи $name: $value")
        }
    }

    @Test
    fun `в русских надписях нет казахских букв`() {
        lines(textsOf(Language.Ru)).forEach { (name, value) ->
            assertTrue(foreign(value).none { it in KAZAKH }, "Ru: казахская буква в надписи $name: $value")
        }
    }

    /**
     * Латиница в казахских надписях — только обозначения, которые так
     * и пишутся на любом языке: названия служб и программ, клавиша,
     * отчёты X и Z, образцы набора и знак подстановки.
     */
    @Test
    fun `в казахских надписях латиница только в допустимых обозначениях`() {
        lines(textsOf(Language.Kk)).forEach { (name, value) ->
            val rest = ALLOWED.fold(value) { left, allowed -> allowed.replace(left, "") }
            assertTrue(rest.none { it in 'A'..'Z' || it in 'a'..'z' }, "Kk: латиница в надписи $name: $value")
        }
    }

    /**
     * Надпись без собственных имён, которые пишутся одинаково на любом языке.
     *
     * Язык назван на нём самом — «Қазақша» и «Русский» в выборе языка чека
     * стоят и на английском: так язык узнают, не зная двух других. Улица
     * в образце набора адреса названа так, как её пишет адресный регистр:
     * подсказка учит набирать именно это написание.
     */
    private fun foreign(value: String): String =
        (Language.entries.map { it.title } + REGISTRY_NAMES).fold(value) { left, name -> left.replace(name, "") }

    private companion object {
        /** Кириллица Unicode целиком: русские и казахские буквы. */
        val CYRILLIC = '\u0400'..'\u04FF'

        /** Образец написания адресного регистра в подсказке к выбору адреса. */
        val REGISTRY_NAMES = listOf("Қабанбай батыр", "Кабанбай")

        /** Буквы казахского алфавита, которых нет в русском. */
        const val KAZAKH = "ӘәҒғҚқҢңӨөҰұҮүҺһІі"

        /**
         * Допустимая латиница в казахских надписях.
         *
         * Названия служб и программ — NCALayer, eGov mobile, SMS, Telegram,
         * WhatsApp; PDF, QR и ID — так их называют и по-казахски; AUTH
         * и `.p12` — так НУЦ называет ключ входа и файл ключа; Enter — надпись
         * на клавише; X и Z — названия отчётов кассы; схемы адреса
         * `http://` и `https://`; образцы набора: адрес почты и номер
         * телефона `+7 7XX XXX XX XX`; места подстановки `%s`, `%1$s`,
         * `{phone}` и `{text}` — их заполняет программа, а не читает кассир.
         */
        val ALLOWED = listOf(
            Regex("""%(\d+\$)?s"""),
            Regex("""\{(phone|text)\}"""),
            Regex("""https?://"""),
            Regex("""name@example\.kz"""),
            Regex("""\d*X{2,3}"""),
            Regex("""\beGov mobile\b"""),
            Regex("""\.p12\b"""),
            Regex("""\b(NCALayer|AUTH|SMS|Telegram|WhatsApp|PDF|PNG|HTML|Android|IP|VPN|DNS|QR|ID|Enter|X|Z)\b""")
        )
    }
}
