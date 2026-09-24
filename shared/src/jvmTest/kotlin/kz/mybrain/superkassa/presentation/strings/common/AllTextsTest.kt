package kz.mybrain.superkassa.presentation.strings.common

import kz.mybrain.superkassa.presentation.common.format.fill
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsRecordTexts
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.analytics.sieveTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.edsTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.machineTexts
import kz.mybrain.superkassa.presentation.strings.debug.debugTexts
import kz.mybrain.superkassa.presentation.strings.journal.journalTexts
import kz.mybrain.superkassa.presentation.strings.journal.ofdRefusalTexts
import kz.mybrain.superkassa.presentation.strings.kassa.blockReasonTexts
import kz.mybrain.superkassa.presentation.strings.kassa.buyerContactTexts
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.strings.kassa.paymentTexts
import kz.mybrain.superkassa.presentation.strings.kassa.saleTexts
import kz.mybrain.superkassa.presentation.strings.map.mapAddressTexts
import kz.mybrain.superkassa.presentation.strings.print.printTexts
import kz.mybrain.superkassa.presentation.strings.settings.coreSettingTexts
import kz.mybrain.superkassa.presentation.strings.settings.kassaFactsTexts
import kz.mybrain.superkassa.presentation.strings.setup.setupTexts
import kz.mybrain.superkassa.presentation.strings.shell.startTexts
import kz.mybrain.superkassa.presentation.strings.shift.coreTexts
import kz.mybrain.superkassa.presentation.strings.update.updateTexts
import kotlin.test.Test
import kotlin.test.assertTrue
import kz.mybrain.superkassa.presentation.strings.settings.deliveryTexts as settingsDeliveryTexts

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
            val name = "${value::class.simpleName}.${field.name}"
            when {
                member is String -> found += name to member
                member is Map<*, *> -> member.values.filterIsInstance<String>().forEach { found += name to it }
                member::class.java.name.startsWith("kz.mybrain") -> found += strings(member, seen)
            }
        }
        return found
    }

    private fun sets(language: Language): List<Any> = listOf(
        stringsOf(language),
        cabinetTexts(language),
        // Плашки шапки и причины блокировки кассы — такие же наборы
        // надписей, а под проверкой их не было: забытый перевод причины
        // доходил до кассира пустым местом там, где сказано, что делать
        // с заблокированной кассой.
        coreTexts(language),
        blockReasonTexts(language),
        analyticsTexts(language),
        machineTexts(language),
        mapAddressTexts(language),
        saleTexts(language),
        paymentTexts(language),
        setupTexts(language),
        debugTexts(language),
        edsTexts(language),
        updateTexts(language)
    ) + laterSets(language)

    /** Наборы, заведённые после первого прохода проверки: их поля под ней так же. */
    private fun laterSets(language: Language): List<Any> = listOf(
        analyticsRecordTexts(language),
        sieveTexts(language),
        journalTexts(language),
        ofdRefusalTexts(language),
        buyerContactTexts(language),
        moneyTexts(language),
        printTexts(language),
        coreSettingTexts(language),
        settingsDeliveryTexts(language),
        kassaFactsTexts(language),
        startTexts(language)
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

    /**
     * Место для значения в надписи — только `%s`: его и заполняет `fill`.
     *
     * Шаблон `%1$s` подстановка не понимала, и кассир видел на экране
     * «Осталось %1$s» вместо отсчёта. Прежняя проверка знала о таком
     * шаблоне, но ничего не утверждала и проходила всегда. Теперь каждая
     * надпись заполняется значениями, и знака подстановки в итоге быть
     * не должно.
     */
    @Test
    fun `подстановки в надписях не остаются на экране`() {
        Language.entries.forEach { language ->
            sets(language).flatMap { strings(it) }.forEach { (name, value) ->
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
