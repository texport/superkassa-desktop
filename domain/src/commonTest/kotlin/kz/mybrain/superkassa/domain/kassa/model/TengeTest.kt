package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Счёт в тиынах по правилу ядра.
 *
 * Строка чека считается так же, как её считает ядро: иначе касса видит
 * одну сумму, а БФД получает другую и отвергает чек несходящейся оплатой.
 */
class TengeTest {

    @Test
    fun `половина тиына в строке чека идёт вверх`() {
        val sum = Tenge.lineSum(33_333L, Decimal.parse("1.5"))
        assertEquals(50_000L, sum)
    }

    @Test
    fun `строка чека учитывает тысячные доли количества`() {
        assertEquals(12_345L, Tenge.lineSum(12_345L, Decimal.parse("1")))
        assertEquals(6_173L, Tenge.lineSum(12_345L, Decimal.parse("0.5")))
        assertEquals(15L, Tenge.lineSum(99L, Decimal.parse("0.155")))
    }

    @Test
    fun `суммы складываются в тиынах без потерь`() {
        val one = Decimal.parse("1")
        val lines = listOf(Tenge.lineSum(10L, one), Tenge.lineSum(20L, one))
        assertEquals(30L, lines.sum())
    }

    @Test
    fun `число ядра переводится в тиыны к ближайшему как у ядра`() {
        assertEquals(1_250L, Tenge.of(Decimal.parse("12.5")))
        assertEquals(1L, Tenge.of(Decimal.parse("0.005")))
        assertEquals(-1L, Tenge.of(Decimal.parse("-0.005")))
    }

    @Test
    fun `для показа доля тиына отбрасывается`() {
        assertEquals(999L, Tenge.truncated(Decimal.parse("9.999")))
        assertEquals(-999L, Tenge.truncated(Decimal.parse("-9.999")))
        assertEquals(45_000L, Tenge.truncated(Decimal.parse("450")))
    }

    @Test
    fun `тиыны уходят в ядро числом с двумя знаками`() {
        assertEquals(Decimal.parse("3955.50"), Tenge.decimal(395_550L))
        assertEquals(2, Tenge.decimal(7L).scale)
    }
}
