package kz.mybrain.superkassa.strings.api

import kz.mybrain.superkassa.strings.lines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ни одной пустой надписи ни в одном наборе и ни на одном языке.
 *
 * Касса и кабинет государственные: экран обязан читаться по-казахски так
 * же полно, как по-русски. Забытая строка не ломает ни сборку, ни тесты
 * области — она молча доходит до кассира пустым местом там, где должно
 * стоять объяснение отказа.
 *
 * Проверка обходит все тексты модуля от точки входа [textsOf]: новое поле
 * попадает под неё само, а список полей руками разошёлся бы с набором
 * на первой же правке.
 */
class AllTextsTest {

    @Test
    fun `каждая надпись заполнена на каждом языке`() {
        Language.entries.forEach { language ->
            lines(textsOf(language)).forEach { (name, value) ->
                assertTrue(value.isNotBlank(), "$language: пустая надпись $name")
            }
        }
    }

    @Test
    fun `наборы не растеряли поля между языками`() {
        val russian = lines(textsOf(Language.Ru)).map { it.first }
        Language.entries.forEach { language ->
            assertEquals(russian, lines(textsOf(language)).map { it.first }, "$language: состав надписей другой")
        }
    }

    /**
     * Место для значения в надписи — только `%s` и `%номер$s`: их и заполняет `fill`.
     *
     * Шаблон, который подстановка не понимает, доходит до кассира знаком:
     * «Осталось %1$s» вместо отсчёта. Каждая надпись заполняется значениями,
     * и знака подстановки в итоге быть не должно.
     */
    @Test
    fun `подстановки в надписях не остаются на экране`() {
        Language.entries.forEach { language ->
            lines(textsOf(language)).forEach { (name, value) ->
                val filled = value.fill(*Array(HOLES) { "X" })
                assertTrue(!FORMAT.containsMatchIn(filled), "$language: $name — осталась подстановка: $filled")
            }
        }
    }

    private companion object {
        /** С запасом больше мест, чем бывает в одной надписи. */
        const val HOLES = 8

        /** Знак подстановки любого вида: `%s`, `%d`, `%1$s`, `%.2f`. */
        val FORMAT = Regex("""%(\d+\$)?[-#+0,(]*\d*(\.\d+)?[sdfxXc]""")
    }
}
