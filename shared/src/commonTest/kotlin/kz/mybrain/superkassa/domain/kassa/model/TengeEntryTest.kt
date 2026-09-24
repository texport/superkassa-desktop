package kz.mybrain.superkassa.domain.kassa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Разбор суммы, набранной кассиром.
 *
 * Сумма разбирается прямо в тиыны: без двоичной дроби и без округления
 * набранного. То, что суммой не является, — `null`, и отказ поля говорит
 * кассиру, что не так.
 */
class TengeEntryTest {

    @Test
    fun `кассир вводит сумму и запятой, и точкой`() {
        assertEquals(1_230L, Tenge.parse("12,30"))
        assertEquals(1_230L, Tenge.parse("12.30"))
        assertEquals(1_230L, Tenge.parse("12,3"))
        assertEquals(1_200L, Tenge.parse("12"))
    }

    @Test
    fun `разряды принимаются обоими пробелами`() {
        assertEquals(120_000L, Tenge.parse("1 200"))
        assertEquals(120_000L, Tenge.parse("1\u00A0200"))
        assertEquals(123_456L, Tenge.parse("1 234,56"))
    }

    @Test
    fun `сумма в поле набрана запятой и без разрядов`() {
        assertEquals("1500,00", Tenge.entered(150_000L))
        assertEquals("0,07", Tenge.entered(7L))
        assertEquals("-12,30", Tenge.entered(-1_230L))
    }

    @Test
    fun `пустой ввод суммой не считается`() {
        assertNull(Tenge.parse(""))
        assertNull(Tenge.parse("   "))
        assertNull(Tenge.parse(","))
        assertNull(Tenge.parse("-"))
    }

    @Test
    fun `не число суммой не считается`() {
        assertNull(Tenge.parse("abc"))
        assertNull(Tenge.parse("не число"))
        assertNull(Tenge.parse("1,2,3"))
        assertNull(Tenge.parse("1e3"))
        assertNull(Tenge.parse("--5"))
    }

    @Test
    fun `доли мельче тиына не принимаются`() {
        assertNull(Tenge.parse("0,005"))
        assertNull(Tenge.parse("10.005"))
    }

    @Test
    fun `ноль и отрицательная сумма разбираются, чтобы отказ назвал их`() {
        assertEquals(0L, Tenge.parse("0"))
        assertEquals(0L, Tenge.parse("0,00"))
        assertEquals(-500L, Tenge.parse("-5"))
        assertEquals(-50L, Tenge.parse("-0,50"))
    }

    @Test
    fun `миллиарды разбираются без потерь`() {
        assertEquals(9_879_703_110_945L, Tenge.parse("98 797 031 109,45"))
        assertEquals(999_999_999_999_999_999L, Tenge.parse("9999999999999999,99"))
    }

    @Test
    fun `сумма больше предела тиынов — опечатка, а не сумма`() {
        assertNull(Tenge.parse("99999999999999999"))
        assertNull(Tenge.parse("99999999999999999999999"))
    }

    @Test
    fun `ведущие нули предела не съедают`() {
        assertEquals(100L, Tenge.parse("000000000000000000001"))
        assertEquals(5L, Tenge.parse(",05"))
    }
}
