package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Границы областей экранов: область не видит внутренностей другой области.
 *
 * Правило и общие полки — [AreaRules]; здесь оно проверяет исходники
 * этого модуля. Области домена проверяет модуль `domain` у себя.
 * Долга у экранов нет: области берут друг у друга только через общие
 * полки, контракты и слоты каркаса.
 *
 * Тем же порядком проверяется шаблон модели экрана: файл `*ViewModel.kt`
 * не видит ни контейнера окна, ни держателя входа, ни портов — только
 * сценарии своей области ([AreaRules.modelViolations]).
 */
class AreaBoundariesTest {

    @Test
    fun `no area imports the inside of another area`() {
        val found = AreaRules.violations(SourceTree.main())
        assertTrue(found.isEmpty(), "area violations:\n" + found.sorted().joinToString("\n"))
    }

    @Test
    fun `view models see only use cases`() {
        assertEquals(emptySet(), AreaRules.modelViolations(SourceTree.main()), "view models reaching past use cases")
    }
}
