package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
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

    private fun values(texts: CabinetTexts): Map<String, String> =
        CabinetTexts::class.java.declaredFields
            .filter { it.type == String::class.java }
            .associate { field ->
                field.isAccessible = true
                field.name to (field.get(texts) as String)
            }

    @Test
    fun `ни одной пустой надписи ни на одном языке`() {
        Language.entries.forEach { language ->
            values(cabinetTexts(language)).forEach { (name, value) ->
                assertTrue(value.isNotBlank(), "$language: пустая надпись $name")
            }
        }
    }

    @Test
    fun `счётчик показанного подставляет оба числа`() {
        Language.entries.forEach { language ->
            val line = cabinetTexts(language).shownOf.format(50, 1240)
            assertTrue(line.contains("50"), "$language: не подставлено показанное — $line")
            assertTrue(line.contains("1240"), "$language: не подставлено общее — $line")
            // Незакрытая подстановка доходит до владельца как «%1$s»:
            // в строке Kotlin доллар начинает шаблон, и забытое экранирование
            // ломается не сборкой, а надписью на экране.
            assertTrue(!line.contains('%'), "$language: подстановка осталась в тексте — $line")
        }
    }
}
