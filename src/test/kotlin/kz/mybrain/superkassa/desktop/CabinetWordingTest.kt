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

    /**
     * Проверяется весь набор, а не три строки из него.
     *
     * Прежде здесь стоял перечень: кнопка создания кассы, кнопка создания
     * точки и подсказка о пустом списке касс. Соседнее с последней поле —
     * сам пустой список — в перечень не попало и говорило «Касс нет —
     * заведите первую»: правило было записано, проверка была зелёной,
     * а владелец читал ровно то, что правило запрещает.
     */
    @Test
    fun `касса и точка создаются, а не заводятся`() {
        lines(cabinetTexts(Language.Ru)).forEach { line ->
            spoken.forEach { word ->
                assertFalse(line.contains(word, ignoreCase = true), "разговорное «$word» в строке: $line")
            }
        }
    }

    /**
     * Все надписи набора, включая вложенные подсказки и названия состояний.
     *
     * Обходом, а не перечнем: перечень отстаёт от набора на каждую новую
     * строку, и отстаёт молча.
     */
    private fun lines(holder: Any): List<String> =
        holder::class.java.declaredFields.flatMap { field ->
            field.isAccessible = true
            when (val held = field.get(holder)) {
                is String -> listOf(held)
                null -> emptyList()
                else -> if (held::class.java.name.startsWith(TEXTS)) lines(held) else emptyList()
            }
        }

    @Test
    fun `токен назван токеном без лишних определений`() {
        val texts = cabinetTexts(Language.Ru)
        assertFalse(texts.token.contains("Технический"), "определение ничего не добавляет: ${texts.token}")
    }

    /** Разговорные формы, которых в государственном кабинете быть не должно. */
    private val spoken = listOf("Завести", "Заведите", "Заведение", "Заведена", "Заведены")

    private companion object {
        /** Откуда берутся вложенные наборы надписей: дальше обход не идёт. */
        const val TEXTS = "kz.mybrain.superkassa.desktop.ui.strings."
    }
}
