package kz.mybrain.superkassa.domain.kassa.model.sale

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Время стоянки, как его набирает кассир. */
class ClockTest {

    @Test
    fun `время стоянки набирают с нулём и без`() {
        assertEquals(LocalTime(9, 30), clockOf("9:30"))
        assertEquals(LocalTime(9, 30), clockOf(" 09:30 "))
        assertEquals(LocalTime(23, 5), clockOf("23:05"))
    }

    @Test
    fun `не время суток не принимается`() {
        assertNull(clockOf("25:00"))
        assertNull(clockOf("9.30"))
        assertNull(clockOf(""))
    }
}
