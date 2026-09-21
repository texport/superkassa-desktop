package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.debugTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.machineTexts
import kz.mybrain.superkassa.desktop.ui.strings.mapAddressTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.strings.setupTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * На экране везде БФД, а не ОФД.
 *
 * Приёмная база называется базой фискальных данных, и владелец читает
 * это имя рядом с формами КГД. Оставшееся «ОФД» читается как другая
 * служба — та, выбор которой из продукта убран.
 *
 * Проверка обходит наборы отражением: новая надпись попадает под неё
 * сама, а список полей руками разошёлся бы с набором на первой правке.
 */
class BfdWordingTest {

    @Test
    fun `ни одной надписи с прежним именем приёмной базы`() {
        Language.entries.forEach { language ->
            sets(language).flatMap { strings(it) }
                .filter { (name, _) -> name.substringAfter('.') !in technicalState }
                .forEach { (name, value) ->
                    forbidden.forEach { word ->
                        assertFalse(value.contains(word), "$language: «$word» в надписи $name: $value")
                    }
                }
        }
    }

    /** Аббревиатура расшифрована, и ровно в одном месте. */
    @Test
    fun `расшифровка стоит один раз на приложение`() {
        assertTrue(
            moneyTexts(Language.Ru).kkm.bfdMeaning.contains("база фискальных данных"),
            "подсказка обязана расшифровывать аббревиатуру"
        )
        val decoded = Language.entries.sumOf { language ->
            sets(language).flatMap { strings(it) }.count { (_, value) -> value.contains("база фискальных данных") }
        }
        assertTrue(decoded == 1, "расшифровка повторяется в надписях $decoded раз")
    }

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
        analyticsTexts(language),
        machineTexts(language),
        mapAddressTexts(language),
        saleTexts(language),
        paymentTexts(language),
        setupTexts(language),
        journalTexts(language),
        moneyTexts(language),
        debugTexts(language)
    )

    /** Слова, которых на экране быть не должно ни на одном языке. */
    private val forbidden = listOf("ОФД", "OFD")

    /**
     * Надписи сверки состояний остаются на прежнем имени.
     *
     * Там названы три службы, которые говорят о кассе порознь, и это
     * ведётся отдельно от переименования интерфейса.
     */
    private val technicalState = setOf("sourceOfd", "technicalUnknownHint", "ofdDisconnected")
}
