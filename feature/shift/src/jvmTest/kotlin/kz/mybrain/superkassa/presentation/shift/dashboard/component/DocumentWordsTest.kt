package kz.mybrain.superkassa.presentation.shift.dashboard.component

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals

/** Сумма документа в строке смены на главном экране. */
class DocumentWordsTest {

    @Test
    fun `у отчёта и открытия смены на главной стоит прочерк, а не ноль`() {
        // Журнал за срок ставит на их месте прочерк, а список документов
        // смены рисовал «0,00 ₸» — кассир читал это как «не продано ничего».
        val report = CoreScene.document("d-x", type = "X_REPORT", amount = 0)
        val opened = CoreScene.document("d-o", type = "SHIFT_OPEN", amount = 0)
        val sale = CoreScene.document("d-s", type = "SALE", amount = 120_000)

        assertEquals(Glyphs.DASH, documentAmount(report))
        assertEquals(Glyphs.DASH, documentAmount(opened))
        assertEquals("1${Glyphs.NBSP}200,00${Glyphs.NBSP}₸", documentAmount(sale))
    }
}
