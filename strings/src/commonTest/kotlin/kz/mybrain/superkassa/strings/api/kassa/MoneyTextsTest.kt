package kz.mybrain.superkassa.strings.api.kassa

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.strings.lines
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
 * это место, которое на экране этого языка останется незаполненным.
 */
class MoneyTextsTest {

    private val languages = Language.entries.associateWith { lines(textsOf(it).kassa.money) }

    @Test
    fun `ни одна надпись не пуста`() {
        languages.forEach { (language, fields) ->
            fields.forEach { (name, value) ->
                assertTrue(value.isNotBlank(), "$language: пустая надпись $name")
            }
        }
    }

    @Test
    fun `подстановки совпадают во всех языках`() {
        val russian = languages.getValue(Language.Ru).toMap()
        languages.forEach { (language, fields) ->
            fields.forEach { (name, value) ->
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
        languages.forEach { (language, fields) ->
            fields.forEach { (name, value) ->
                FOREIGN.forEach { foreign ->
                    assertFalse(
                        value.contains(foreign, ignoreCase = true),
                        "$language: в надписи $name чужое понятие «$foreign»"
                    )
                }
            }
        }
    }

    @Test
    fun `языки не повторяют друг друга`() {
        val russian = languages.getValue(Language.Ru)
        assertTrue(russian != languages.getValue(Language.Kk))
        assertTrue(russian != languages.getValue(Language.En))
    }

    /** Сколько подстановок ждёт надпись при показе. */
    private fun placeholders(text: String): Int = PLACEHOLDER.findAll(text).count()

    private companion object {
        val PLACEHOLDER = Regex("%s")

        /**
         * Понятия чужих налоговых систем.
         *
         * Касса работает в Казахстане: валюта — тенге, сотая доля — тиын,
         * надзор — КГД. Рубли и копейки здесь означают, что надпись писали
         * с оглядкой на другую страну. Сокращения чужих учреждений и
         * идентификаторов проверяются по всем текстам модуля целиком.
         */
        val FOREIGN = listOf("рубл", "копей", "копее", "ruble", "kopeck")
    }
}
