package kz.mybrain.superkassa.presentation.kassa.sale.component

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kotlin.test.Test
import kotlin.test.assertEquals

/** Доля в процентах словами экрана: без лишних нулей и со знаком при числе. */
class PercentTextTest {

    @Test
    fun `лишние нули отброшены, дробь — запятой`() {
        assertEquals("10${Glyphs.NBSP}%", formatPercent(decimal("10.00")))
        assertEquals("12,5${Glyphs.NBSP}%", formatPercent(decimal("12.50")))
        assertEquals("100${Glyphs.NBSP}%", formatPercent(decimal("100")))
        assertEquals("0${Glyphs.NBSP}%", formatPercent(decimal("0.00")))
    }
}
