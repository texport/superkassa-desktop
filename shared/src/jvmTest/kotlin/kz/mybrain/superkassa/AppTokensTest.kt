package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Размеры оформления в точках сборки — только из дизайн-системы.
 *
 * Числа в точках (`16.dp`) и в пунктах шрифта (`15.sp`) пишутся в одном
 * месте — в токенах модуля `designsystem`. Точки сборки платформенных
 * приложений берут готовое, как и экраны: отступ, записанный числом на
 * месте, расходился с соседним экраном на пару точек. Правило — у
 * [DesignRules]; модули экранов проверяют себя сами, токены и компоненты —
 * дизайн-система у себя. Своих проверок у точек сборки нет, и их исходники
 * читает каркас, который они собирают.
 */
class AppTokensTest {

    @Test
    fun `в точках сборки нет чисел в точках и пунктах`() {
        val found = MODULES.flatMap { DesignRules.code(it).entries }
            .flatMap { (path, lines) -> DesignRules.literals(path, lines) }
        assertTrue(found.isEmpty(), "числа размеров в точках сборки:\n" + found.joinToString("\n"))
    }

    private companion object {
        /** Точки сборки платформенных приложений. */
        val MODULES = listOf("../desktopApp", "../androidApp")
    }
}
