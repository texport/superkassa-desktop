package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsRecordTexts
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Комитет государственных доходов назван в разделе одним словом.
 *
 * На казахских надписях вкладки учёта стояли рядом «МКК-ға өтініш
 * берілген» и «МКД бас тартты»: один и тот же комитет под двумя
 * сокращениями, причём второго не существует. В пустом состоянии
 * карты к ним добавлялось русское «КГД» посреди казахской фразы.
 * Гость, которому этот комитет подчинён, прочтёт такое первым.
 *
 * Заодно проверяется, что в разделе нет обозначений чужой юрисдикции:
 * касса, протокол и все надписи относятся к Казахстану.
 */
class AnalyticsWordsTest {

    /** Все надписи раздела одной строкой: набор — data class, и печатает он свои поля. */
    private fun words(language: Language): String =
        analyticsTexts(language).toString() + analyticsRecordTexts(language).toString()

    @Test
    fun `комитет назван одним сокращением на каждом языке`() {
        NAMES.forEach { (language, right) ->
            val said = words(language)
            assertTrue(said.contains(right), "$language: комитет не назван вовсе")
            (NAMES.values.toSet() + WRONG - right).forEach { other ->
                assertFalse(said.contains(other), "$language: комитет назван ещё и «$other»")
            }
        }
    }

    @Test
    fun `в разделе нет обозначений чужой юрисдикции`() {
        Language.entries.forEach { language ->
            val said = words(language).lowercase()
            FOREIGN.forEach { word ->
                assertFalse(said.contains(word), "$language: в надписях раздела встретилось «$word»")
            }
        }
    }

    private companion object {
        /** Как комитет зовётся на каждом языке. */
        val NAMES = mapOf(
            Language.Ru to "КГД",
            Language.Kk to "МКК",
            Language.En to "KGD"
        )

        /** Сокращения, которых не существует вовсе. */
        val WRONG = setOf("МКД")

        /** Учреждения, валюта и идентификаторы чужой страны. */
        val FOREIGN = listOf("фнс", "fns", "инн ", "рубл", "копей")
    }
}
