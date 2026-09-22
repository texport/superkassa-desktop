package kz.mybrain.superkassa.desktop

import androidx.compose.ui.text.AnnotatedString
import kz.mybrain.superkassa.desktop.ui.components.GroupedAmount
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Поле суммы разбивает разряды по ходу набора.
 *
 * Без разбивки кассир считал цифры глазами, чтобы отличить полторы тысячи
 * от полутора миллионов. Разбивка идёт показом, а место курсора считается
 * заново: иначе он прыгал в конец при правке цифры в середине суммы.
 */
class MoneyFieldTest {

    @Test
    fun `набранное показывается разрядами`() {
        assertEquals("1 500 000", shownOf("1500000"))
        assertEquals("1 500,50", shownOf("1500,50"))
        assertEquals("500", shownOf("500"))
    }

    @Test
    fun `курсор стоит у той же цифры, что и в набранном`() {
        val grouped = GroupedAmount.filter(AnnotatedString("1500000")).offsetMapping

        assertEquals(0, grouped.originalToTransformed(0))
        assertEquals(2, grouped.originalToTransformed(1))
        assertEquals("1 500 000".length, grouped.originalToTransformed("1500000".length))
        assertEquals(1, grouped.transformedToOriginal(2))
        assertEquals("1500000".length, grouped.transformedToOriginal("1 500 000".length))
    }

    private fun shownOf(raw: String): String = GroupedAmount.filter(AnnotatedString(raw)).text.text
}
