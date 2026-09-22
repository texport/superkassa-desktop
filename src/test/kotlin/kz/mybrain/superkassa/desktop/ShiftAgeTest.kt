package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.dashboard.shiftTooLong
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Сутки открытой смены — предел, по которому узел блокирует кассу.
 *
 * Главный экран молчал о смене, идущей вторые сутки, и кассир узнавал
 * о пределе из отказа на первом чеке следующего утра, когда касса уже
 * заблокирована и пробить чек нечем.
 */
class ShiftAgeTest {

    private val opened = 1_789_000_000_000L

    @Test
    fun `смена, открытая час назад, не считается затянувшейся`() {
        assertFalse(shiftTooLong(opened, opened + HOUR))
    }

    @Test
    fun `ровно сутки — уже предел`() {
        assertTrue(shiftTooLong(opened, opened + DAY))
        assertTrue(shiftTooLong(opened, opened + DAY + HOUR))
    }

    /** Смены, о которой узел не сказал, нет и срока: домысливать нечего. */
    @Test
    fun `о смене без времени открытия ничего не утверждается`() {
        assertFalse(shiftTooLong(null, opened + DAY + DAY))
    }

    private companion object {
        const val HOUR = 60L * 60 * 1000
        const val DAY = 24 * HOUR
    }
}
