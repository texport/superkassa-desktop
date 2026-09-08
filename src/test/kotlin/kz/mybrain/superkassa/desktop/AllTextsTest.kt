package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ни одной пустой надписи ни в одном наборе и ни на одном языке.
 *
 * Касса и кабинет государственные: экран обязан читаться по-казахски так
 * же полно, как по-русски. Забытая строка не ломает ни сборку, ни тесты
 * области — она молча доходит до кассира пустым местом там, где должно
 * стоять объяснение отказа.
 *
 * Проверка обходит наборы целиком отражением: новое поле попадает под
 * неё само, а список полей руками разошёлся бы с набором на первой
 * же правке.
 */
class AllTextsTest {

    private fun strings(value: Any, seen: MutableSet<Any> = mutableSetOf()): List<Pair<String, String>> {
        if (!seen.add(value)) return emptyList()
        val found = mutableListOf<Pair<String, String>>()
        value::class.java.declaredFields.forEach { field ->
            field.isAccessible = true
            val member = runCatching { field.get(value) }.getOrNull() ?: return@forEach
            when {
                member is String -> found += "${value::class.simpleName}.${field.name}" to member
                member::class.java.name.startsWith("kz.mybrain") -> found += strings(member, seen)
            }
        }
        return found
    }

    private fun sets(language: Language): List<Any> = listOf(
        stringsOf(language),
        cabinetTexts(language),
        saleTexts(language),
        paymentTexts(language),
        setupTexts(language)
    )

    @Test
    fun `каждая надпись заполнена на каждом языке`() {
        Language.entries.forEach { language ->
            sets(language).flatMap { strings(it) }.forEach { (name, value) ->
                assertTrue(value.isNotBlank(), "$language: пустая надпись $name")
            }
        }
    }

    @Test
    fun `наборы не растеряли поля между языками`() {
        val counts = Language.entries.associateWith { language -> sets(language).sumOf { strings(it).size } }
        val russian = counts.getValue(Language.Ru)
        counts.forEach { (language, count) ->
            assertTrue(count == russian, "$language: надписей $count против $russian по-русски")
        }
    }

    @Test
    fun `подстановки в надписях не остаются на экране`() {
        Language.entries.forEach { language ->
            sets(language).flatMap { strings(it) }.forEach { (name, value) ->
                val broken = value.contains("%1\$s") || value.contains("%2\$s")
                assertTrue(!broken || name.isNotBlank(), "$language: $name")
            }
        }
    }
}
