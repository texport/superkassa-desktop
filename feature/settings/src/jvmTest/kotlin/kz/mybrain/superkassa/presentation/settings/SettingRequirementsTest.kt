package kz.mybrain.superkassa.presentation.settings

import kz.mybrain.superkassa.domain.settings.model.KkmDemand
import kz.mybrain.superkassa.domain.settings.model.KkmNeed
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Невыполненное требование видно без цвета.
 *
 * Плашки требований различались одной заливкой: подпись «Смена закрыта»
 * стоит и на выполненном, и на невыполненном. Основной тон кассы
 * выбирается из четырнадцати, и по мерке ΔE янтарный сходится с цветом
 * ожидания (0,7), зелёный — с цветом доставки (5,2), красный — с цветом
 * отказа (8,1) при пороге различения около двойки: на такой кассе цвет
 * плашки перестаёт отвечать, чего не хватает.
 */
class SettingRequirementsTest {

    @Test
    fun `выполненное и невыполненное требование различаются надписью`() {
        Language.entries.forEach { language ->
            val texts = textsOf(language).kassa.money.kkm
            KkmDemand.entries.forEach { demand ->
                val met = requirementLine(KkmNeed(demand, met = true), texts)
                val unmet = requirementLine(KkmNeed(demand, met = false), texts)
                assertNotEquals(met, unmet, "$language: $demand читается одинаково без цвета")
                assertTrue(demand.title(texts) in met, "$language: $demand потерял своё название")
            }
        }
    }
}
