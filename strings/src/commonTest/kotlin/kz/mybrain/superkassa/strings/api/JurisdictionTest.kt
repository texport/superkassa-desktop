package kz.mybrain.superkassa.strings.api

import kz.mybrain.superkassa.strings.lines
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Надписи говорят только о Казахстане.
 *
 * Касса работает в Казахстане: валюта — тенге, сотая доля — тиын,
 * регистрационный номер выдаёт КГД, идентификаторы — БИН и ИИН. Рубли,
 * копейки, ИНН и ФНС на экране значат, что надпись писали с оглядкой
 * на другую страну.
 */
class JurisdictionTest {

    @Test
    fun `нет ни российской валюты ни учреждения ни идентификатора`() {
        Language.entries.forEach { language ->
            lines(textsOf(language)).forEach { (name, value) ->
                val words = wordsOf(value)
                FOREIGN.forEach { foreign ->
                    assertTrue(words.none { foreign(it) }, "$language: чужое обозначение в надписи $name: $value")
                }
            }
        }
    }

    /** Слова надписи строчными буквами: разбиение по всему, что не буква. */
    private fun wordsOf(text: String): List<String> = buildList {
        val word = StringBuilder()
        (text.lowercase() + " ").forEach { char ->
            if (char.isLetter()) {
                word.append(char)
            } else if (word.isNotEmpty()) {
                add(word.toString())
                word.clear()
            }
        }
    }

    private companion object {
        /** Слова чужой юрисдикции: целиком — сокращения, по началу — валюта. */
        val FOREIGN: List<(String) -> Boolean> = listOf(
            { it == "инн" },
            { it == "фнс" },
            { it == "inn" },
            { it == "fns" },
            { it.startsWith("рубл") },
            { it.startsWith("копе") },
            { it.startsWith("ruble") || it.startsWith("rouble") },
            { it.startsWith("kopeck") || it.startsWith("kopek") }
        )
    }
}
