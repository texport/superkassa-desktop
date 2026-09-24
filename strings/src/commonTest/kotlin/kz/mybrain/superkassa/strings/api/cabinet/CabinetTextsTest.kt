package kz.mybrain.superkassa.strings.api.cabinet

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.strings.lines
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Надписи личного кабинета.
 *
 * Кабинет государственный: экран обязан читаться на казахском так же
 * полно, как на русском. Пустая или забытая строка — это владелец,
 * которому нечего прочитать.
 */
class CabinetTextsTest {

    @Test
    fun `ни одной пустой надписи ни на одном языке`() {
        Language.entries.forEach { language ->
            lines(textsOf(language).cabinet).forEach { (name, value) ->
                assertTrue(value.isNotBlank(), "$language: пустая надпись $name")
            }
        }
    }

    @Test
    fun `счётчик показанного подставляет оба числа`() {
        Language.entries.forEach { language ->
            val line = textsOf(language).cabinet.shownOf.fill(50, 1240)
            assertTrue(line.contains("50"), "$language: не подставлено показанное — $line")
            assertTrue(line.contains("1240"), "$language: не подставлено общее — $line")
            // Незакрытая подстановка доходит до владельца как «%1$s»:
            // в строке Kotlin доллар начинает шаблон, и забытое экранирование
            // ломается не сборкой, а надписью на экране.
            assertTrue(!line.contains('%'), "$language: подстановка осталась в тексте — $line")
        }
    }
}
