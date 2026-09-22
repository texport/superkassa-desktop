package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.settings.KkmDemand
import kz.mybrain.superkassa.desktop.ui.settings.KkmNeed
import kz.mybrain.superkassa.desktop.ui.settings.requirementLine
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
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
            val texts = moneyTexts(language).kkm
            KkmDemand.entries.forEach { demand ->
                val met = requirementLine(KkmNeed(demand, met = true), texts)
                val unmet = requirementLine(KkmNeed(demand, met = false), texts)
                assertNotEquals(met, unmet, "$language: $demand читается одинаково без цвета")
                assertTrue(demand.title(texts) in met, "$language: $demand потерял своё название")
            }
        }
    }
}
