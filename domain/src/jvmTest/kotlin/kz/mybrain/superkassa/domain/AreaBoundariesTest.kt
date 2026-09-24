package kz.mybrain.superkassa.domain

import kz.mybrain.superkassa.AreaRules
import kz.mybrain.superkassa.SourceTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Границы областей домена: область не видит внутренностей другой области.
 *
 * Правило и общие полки — [AreaRules]; области экранов проверяет `shared`.
 * Нарушения, с которыми код пришёл к проверке, перечислены в
 * `area-debt.txt`: новое падает сразу, исправленное требует вычеркнуть строку.
 */
class AreaBoundariesTest {

    @Test
    fun `no area imports the inside of another area`() {
        val fresh = violations() - SourceTree.debt(AREA_DEBT)
        assertTrue(fresh.isEmpty(), "new area violations:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `debt lists only violations that still exist`() {
        val paid = SourceTree.debt(AREA_DEBT) - violations()
        assertEquals(emptySet(), paid, "violations fixed, remove them from $AREA_DEBT")
    }

    private fun violations(): Set<String> = AreaRules.violations(SourceTree.main())

    private companion object {
        const val AREA_DEBT = "area-debt.txt"
    }
}
