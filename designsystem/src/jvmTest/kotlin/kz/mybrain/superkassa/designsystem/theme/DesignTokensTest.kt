package kz.mybrain.superkassa.designsystem.theme

import kz.mybrain.superkassa.DesignRules
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Размеры оформления — только из общей шкалы.
 *
 * Числа в точках (`16.dp`) и в пунктах шрифта (`15.sp`) пишутся в одном
 * месте: шаг шкалы — в `theme/size/Spacing.kt`, шрифтовая шкала —
 * в `theme/type`. Компоненты и наборы размеров берут готовое: отступ,
 * записанный числом на месте, расходился с соседним экраном на пару
 * точек, и найти причину в десятках файлов было нельзя. Правило — у
 * [DesignRules]; экраны приложения проверяет модуль `shared` у себя.
 */
class DesignTokensTest {

    @Test
    fun `вне оформления нет чисел в точках и пунктах`() {
        val found = DesignRules.code().filterKeys { !it.startsWith(DesignRules.THEME) }
            .flatMap { (path, lines) -> DesignRules.literals(path, lines) }
        assertTrue(found.isEmpty(), "числа размеров вне theme:\n" + found.joinToString("\n"))
    }

    @Test
    fun `наборы размеров ссылаются на шаг шкалы, а не заводят свои числа`() {
        val found = DesignRules.code().filterKeys { it.startsWith(SIZES) && !it.endsWith("/Spacing.kt") }
            .flatMap { (path, lines) -> DesignRules.literals(path, lines) }
        assertTrue(found.isEmpty(), "свои числа в наборах размеров:\n" + found.joinToString("\n"))
    }

    private companion object {
        const val SIZES = "${DesignRules.THEME}size/"
    }
}
