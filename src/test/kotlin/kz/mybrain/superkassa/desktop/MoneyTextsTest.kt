package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.MoneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Надписи области «Деньги, кассиры и настройки» на трёх языках.
 *
 * Компилятор следит за тем, чтобы поле существовало во всех трёх языках,
 * но не за тем, чтобы оно было переведено, не пусто и не содержало чужой
 * валюты. Отдельно проверяются подстановки: лишний `%s` в одном языке —
 * это исключение при показе, которое увидит только тот, кто на этом языке
 * работает.
 */
class MoneyTextsTest {

    private val languages = Language.entries.associateWith { moneyTexts(it) }

    @Test
    fun `ни одна надпись не пуста`() {
        languages.forEach { (language, texts) ->
            fields(texts).forEach { (name, value) ->
                assertTrue(value.isNotBlank(), "$language: пустая надпись $name")
            }
        }
    }

    @Test
    fun `подстановки совпадают во всех языках`() {
        val russian = fields(languages.getValue(Language.Ru)).toMap()
        languages.forEach { (language, texts) ->
            fields(texts).forEach { (name, value) ->
                assertEquals(
                    placeholders(russian.getValue(name)),
                    placeholders(value),
                    "$language: в надписи $name другое число подстановок"
                )
            }
        }
    }

    @Test
    fun `валюта только казахстанская`() {
        languages.forEach { (language, texts) ->
            fields(texts).forEach { (name, value) ->
                FOREIGN.forEach { foreign ->
                    assertFalse(
                        foreign.containsMatchIn(value),
                        "$language: в надписи $name чужое понятие «${foreign.pattern}»"
                    )
                }
            }
        }
    }

    @Test
    fun `языки не повторяют друг друга`() {
        val russian = languages.getValue(Language.Ru)
        assertTrue(fields(russian) != fields(languages.getValue(Language.Kk)))
        assertTrue(fields(russian) != fields(languages.getValue(Language.En)))
    }

    /**
     * Все надписи набора парами «имя поля — значение».
     *
     * Читается отражением: перечислять полсотни полей руками — значит
     * забыть половину при следующем добавлении.
     */
    private fun fields(texts: MoneyTexts): List<Pair<String, String>> =
        listOf("drawer" to texts.drawer, "cashiers" to texts.cashiers, "kkm" to texts.kkm)
            .flatMap { (group, value) -> stringsOf(value).map { (name, text) -> "$group.$name" to text } }

    private fun stringsOf(group: Any): List<Pair<String, String>> =
        group.javaClass.declaredFields
            .filter { it.type == String::class.java }
            .map { field ->
                field.isAccessible = true
                field.name to (field.get(group) as String)
            }

    /** Сколько подстановок ждёт надпись при показе. */
    private fun placeholders(text: String): Int = PLACEHOLDER.findAll(text).count()

    private companion object {
        val PLACEHOLDER = Regex("%s")

        /**
         * Понятия чужих налоговых систем.
         *
         * Касса работает в Казахстане: валюта — тенге, сотая доля — тиын,
         * надзор — КГД. Рубли, копейки, ИНН и ФНС здесь означают, что
         * надпись писали с оглядкой на другую страну.
         */
        val FOREIGN = listOf(
            Regex("рубл", RegexOption.IGNORE_CASE),
            Regex("копе[йи]", RegexOption.IGNORE_CASE),
            Regex("(?<![а-яё])инн(?![а-яё])", RegexOption.IGNORE_CASE),
            Regex("(?<![а-яё])фнс(?![а-яё])", RegexOption.IGNORE_CASE),
            Regex("ruble", RegexOption.IGNORE_CASE),
            Regex("kopeck", RegexOption.IGNORE_CASE)
        )
    }
}
