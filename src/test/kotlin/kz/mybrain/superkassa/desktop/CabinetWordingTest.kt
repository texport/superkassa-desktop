package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Слова кабинета — государственные, а не разговорные.
 *
 * Кабинет ОФД ведёт учёт контрольно-кассовых машин, и владелец читает
 * его рядом с формами КГД. «Завести кассу» там неуместно так же, как
 * было бы неуместно в самом заявлении.
 */
class CabinetWordingTest {

    @Test
    fun `касса и точка создаются, а не заводятся`() {
        val texts = cabinetTexts(Language.Ru)
        listOf(texts.addRegister, texts.addPlace, texts.hints.registersEmpty).forEach { line ->
            spoken.forEach { word ->
                assertFalse(line.contains(word, ignoreCase = true), "разговорное «$word» в строке: $line")
            }
        }
    }

    @Test
    fun `токен назван токеном без лишних определений`() {
        val texts = cabinetTexts(Language.Ru)
        assertFalse(texts.token.contains("Технический"), "определение ничего не добавляет: ${texts.token}")
    }

    /** Разговорные формы, которых в государственном кабинете быть не должно. */
    private val spoken = listOf("Завести", "Заведите", "Заведение")
}
