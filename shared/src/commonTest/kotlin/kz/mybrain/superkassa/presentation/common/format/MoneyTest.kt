package kz.mybrain.superkassa.presentation.common.format

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals("1\u00A0234,56\u00A0₸", Money.formatTiyn(123_456L))
        assertEquals("1\u00A0000\u00A0000,00\u00A0₸", Money.formatTiyn(100_000_000L))
        assertEquals("0,05\u00A0₸", Money.formatTiyn(5L))
        assertEquals("0,00\u00A0₸", Money.formatTiyn(0L))
    }

    @Test
    fun `миллиарды не выходят из разрядов`() {
        assertEquals("98\u00A0797\u00A0031\u00A0109,45\u00A0₸", Money.formatTiyn(9_879_703_110_945L))
    }

    @Test
    fun `лишние доли числа ядра усекаются вниз, а не округляются вверх`() {
        assertEquals("9,99\u00A0₸", Money.format(Decimal.parse("9.999")))
        assertEquals("450,00\u00A0₸", Money.format(Decimal.parse("450")))
        assertEquals("\u22129,99\u00A0₸", Money.format(Decimal.parse("-9.999")))
    }

    @Test
    fun `отрицательная сумма сохраняет знак`() {
        assertEquals("\u22121\u00A0500,00\u00A0₸", Money.formatTiyn(-150_000L))
    }

    /**
     * У суммы меньше тенге целая часть нулевая, и знак пропадал вместе
     * с ней: сторно на полтиына выглядело обычной продажей.
     */
    @Test
    fun `минус сохраняется у суммы меньше тенге`() {
        assertEquals("\u22120,50\u00A0₸", Money.formatTiyn(-50L))
        assertEquals("\u22120,01\u00A0₸", Money.formatTiyn(-1L))
    }

    @Test
    fun `отсутствие суммы показывается прочерком`() {
        assertEquals("—", Money.formatTiyn(null))
        assertEquals("—", Money.format(null))
    }

    @Test
    fun `сумма из журнала приходит в тиынах`() {
        assertEquals("3\u00A0955,50\u00A0₸", Money.formatTiyn(395_550L))
        assertEquals("0,07\u00A0₸", Money.formatTiyn(7L))
    }

    @Test
    fun `разбитую по разрядам сумму поле же и принимает`() {
        assertEquals(1_137_250L, Tenge.parse(Money.grouped("11372,50")))
    }

    /**
     * Поле суммы показывало «11372,50» рядом с подписью «13 860,00 ₸»:
     * одно и то же число было набрано на экране двумя способами.
     */
    @Test
    fun `набранная сумма разбита теми же разрядами, что и показанная`() {
        assertEquals("11\u00A0372,50", Money.grouped("11372,50"))
        assertEquals("13\u00A0860,00\u00A0₸", Money.formatTiyn(1_386_000L))
    }

    @Test
    fun `недобранная сумма остаётся такой, какой её набирают`() {
        assertEquals("1\u00A0200,", Money.grouped("1200,"))
        assertEquals("не число", Money.grouped("не число"))
    }

    @Test
    fun `счёт штук разбит разрядами без валюты`() {
        assertEquals("12\u00A0845", Money.count(12_845))
        assertEquals("7", Money.count(7L))
        assertEquals("-1\u00A0234", Money.count(-1_234))
    }

    @Test
    fun `количество показывается без незначащих нулей`() {
        assertEquals("2", Money.quantity(Decimal.parse("2.000")))
        assertEquals("0.5", Money.quantity(Decimal.parse("0.500")))
        assertEquals("12", Money.quantity(Decimal.parse("12")))
        assertEquals("—", Money.quantity(null))
    }
}
