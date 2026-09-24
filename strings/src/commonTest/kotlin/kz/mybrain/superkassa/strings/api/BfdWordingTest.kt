package kz.mybrain.superkassa.strings.api

import kz.mybrain.superkassa.strings.lines
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
 * Проверка обходит все тексты модуля: новая надпись попадает под неё
 * сама, а список полей руками разошёлся бы с набором на первой правке.
 */
class BfdWordingTest {

    @Test
    fun `ни одной надписи с прежним именем приёмной базы`() {
        Language.entries.forEach { language ->
            lines(textsOf(language)).forEach { (name, value) ->
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
            textsOf(Language.Ru).kassa.money.kkm.bfdMeaning.contains(DECODED),
            "подсказка обязана расшифровывать аббревиатуру"
        )
        Language.entries.forEach { language ->
            lines(textsOf(language))
                .filter { (_, value) -> value.contains(DECODED) }
                .forEach { (name, value) ->
                    assertTrue(explaining(name), "$language: расшифровка в обычной надписи $name: $value")
                }
        }
    }

    /**
     * Объясняющая ли это надпись.
     *
     * Объяснения живут либо в группе подсказок кабинета, либо в поле,
     * названном подсказкой: и то и другое выходит на экран под значком
     * у заголовка, а не строкой, которую читают каждый день.
     */
    private fun explaining(name: String): Boolean =
        name.contains("hints.") || name.contains("Hint") || name.contains("Meaning")

    /** Слова, которых на экране быть не должно ни на одном языке. */
    private val forbidden = listOf("ОФД", "OFD")

    private companion object {
        /** Расшифровка аббревиатуры БФД. */
        const val DECODED = "база фискальных данных"
    }
}
