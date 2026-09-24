package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Размеры оформления — только из дизайн-системы.
 *
 * Числа в точках (`16.dp`) и в пунктах шрифта (`15.sp`) пишутся в одном
 * месте — в токенах модуля `designsystem`. Экраны и точки сборки берут
 * готовое: отступ, записанный числом на месте, расходился с соседним
 * экраном на пару точек, и найти причину в десятках файлов было нельзя.
 * Правило — у [DesignRules]; токены и компоненты проверяет дизайн-система
 * у себя.
 */
class DesignTokensTest {

    @Test
    fun `в экранах и точках сборки нет чисел в точках и пунктах`() {
        val found = MODULES.flatMap { DesignRules.code(it).entries }
            .flatMap { (path, lines) -> DesignRules.literals(path, lines) }
        assertTrue(found.isEmpty(), "числа размеров вне дизайн-системы:\n" + found.joinToString("\n"))
    }

    private companion object {
        /** Экраны — этот модуль — и точки сборки платформенных приложений. */
        val MODULES = listOf(".", "../desktopApp", "../androidApp")
    }
}
