package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.components.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Деньги на экране.
 *
 * Проверяется усечение вниз, а не округление: касса показывает ровно ту
 * сумму, которую отправила в ОФД, и лишний тиын на экране — это расхождение
 * с фискальным документом.
 */
class MoneyTest {

    @Test
    fun `сумма показывается с тиынами и разделением разрядов`() {
        assertEquals("1\u00A0234,56\u00A0₸", Money.format(BigDecimal("1234.56")))
        assertEquals("1\u00A0000\u00A0000,00\u00A0₸", Money.format(BigDecimal("1000000")))
        assertEquals("0,05\u00A0₸", Money.format(BigDecimal("0.05")))
    }

    @Test
    fun `лишние доли усекаются вниз, а не округляются вверх`() {
        assertEquals("9,99\u00A0₸", Money.format(BigDecimal("9.999")))
    }

    @Test
    fun `отрицательная сумма сохраняет знак`() {
        assertEquals("-1\u00A0500,00\u00A0₸", Money.format(BigDecimal("-1500")))
    }

    /**
     * У суммы меньше тенге целая часть нулевая, и знак пропадал вместе
     * с ней: сторно на полтиына выглядело обычной продажей.
     */
    @Test
    fun `минус сохраняется у суммы меньше тенге`() {
        assertEquals("-0,50\u00A0₸", Money.format(BigDecimal("-0.50")))
        assertEquals("-0,01\u00A0₸", Money.formatTiyn(-1L))
    }

    @Test
    fun `отсутствие суммы показывается прочерком`() {
        assertEquals("—", Money.formatTiyn(null))
    }

    @Test
    fun `сумма из журнала приходит в тиынах`() {
        assertEquals("3\u00A0955,50\u00A0₸", Money.formatTiyn(395_550L))
        assertEquals("0,07\u00A0₸", Money.formatTiyn(7L))
    }

    @Test
    fun `возврат оформляется от суммы чека в тенге`() {
        assertEquals(BigDecimal("3955.50"), Money.tengeOf(395_550L))
    }

    @Test
    fun `кассир вводит сумму и запятой, и точкой`() {
        assertEquals(BigDecimal("12.30"), Money.parse("12,30"))
        assertEquals(BigDecimal("12.30"), Money.parse("12.30"))
        assertEquals(BigDecimal("1200"), Money.parse("1 200"))
        assertEquals(BigDecimal("1200"), Money.parse("1\u00A0200"))
    }

    @Test
    fun `доли мельче тиына не принимаются`() {
        assertNull(Money.parse("10.005"))
    }

    @Test
    fun `пустой ввод суммой не считается`() {
        assertNull(Money.parse("   "))
        assertNull(Money.parse("не число"))
    }
}
