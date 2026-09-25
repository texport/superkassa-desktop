package kz.mybrain.superkassa.strings.api.cabinet

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.strings.lines
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Слова кабинета — государственные, а не разговорные.
 *
 * Кабинет ведёт учёт контрольно-кассовых машин, и владелец читает его
 * рядом с формами КГД. «Завести кассу» там неуместно так же, как было бы
 * неуместно в самом заявлении.
 */
class CabinetWordingTest {

    /**
     * Проверяется весь набор, включая вложенные подсказки и названия
     * состояний, а не три строки из него.
     *
     * Прежде здесь стоял перечень: кнопка создания кассы, кнопка создания
     * точки и подсказка о пустом списке касс. Соседнее с последней поле —
     * сам пустой список — в перечень не попало и говорило «Касс нет —
     * заведите первую»: правило было записано, проверка была зелёной,
     * а владелец читал ровно то, что правило запрещает.
     */
    @Test
    fun `касса и точка создаются — а не заводятся`() {
        val own = lines(textsOf(Language.Ru).cabinet).filterNot { (name, _) -> name.substringBefore('.') in OWN_SETS }
        own.forEach { (name, line) ->
            spoken.forEach { word ->
                assertFalse(line.contains(word, ignoreCase = true), "разговорное «$word» в строке $name: $line")
            }
        }
    }

    @Test
    fun `токен назван токеном без лишних определений`() {
        val texts = textsOf(Language.Ru).cabinet
        val token = texts.register.token
        assertFalse(token.contains("Технический"), "определение ничего не добавляет: $token")
    }

    /** Разговорные формы, которых в государственном кабинете быть не должно. */
    private val spoken = listOf("Завести", "Заведите", "Заведение", "Заведена", "Заведены")

    private companion object {
        /**
         * Свои наборы, вложенные в кабинет одним полем: ожидание подписи
         * и работа кассы на этой машине. Правило о словах учёта под них
         * не заводилось — они говорят не о записи в кабинете, а о машине.
         */
        val OWN_SETS = setOf("eds", "machine")
    }
}
