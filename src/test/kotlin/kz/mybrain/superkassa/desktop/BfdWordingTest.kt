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
                .forEach { (name, value) ->
                    forbidden.forEach { word ->
                        assertFalse(value.contains(word), "$language: «$word» в надписи $name: $value")
                    }
                }
        }
    }

    /**
     * Аббревиатура расшифрована — и только в объяснениях.
     *
     * Расшифровка нужна там, где владелец спрашивает «что это такое»:
     * под значком подсказки. В обычной надписи — на кнопке, в подписи
     * поля, в плашке состояния — она лишняя: экран читают каждый день,
     * а расшифровку один раз. Мест с объяснением больше одного намеренно:
     * подсказку карточки сверки и подсказку технического состояния читают
     * на разных экранах, и отсылать со второго на первый было бы издёвкой.
     */
    @Test
    fun `расшифровка стоит только в объяснениях`() {
        assertTrue(
            moneyTexts(Language.Ru).kkm.bfdMeaning.contains("база фискальных данных"),
            "подсказка обязана расшифровывать аббревиатуру"
        )
        Language.entries.forEach { language ->
            sets(language).flatMap { strings(it) }
                .filter { (_, value) -> value.contains("база фискальных данных") }
                .forEach { (name, value) ->
                    assertTrue(explaining(name), "$language: расшифровка в обычной надписи $name: $value")
                }
        }
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

    /**
     * Объясняющая ли это надпись.
     *
     * Объяснения живут либо в группе подсказок кабинета, либо в поле,
     * названном подсказкой: и то и другое выходит на экран под значком
     * у заголовка, а не строкой, которую читают каждый день.
     */
    private fun explaining(name: String): Boolean =
        name.contains("Hints.") || name.contains("Hint") || name.contains("Meaning")

    /** Слова, которых на экране быть не должно ни на одном языке. */
    private val forbidden = listOf("ОФД", "OFD")
}
