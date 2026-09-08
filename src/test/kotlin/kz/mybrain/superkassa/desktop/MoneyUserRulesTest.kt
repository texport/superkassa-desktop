package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.ui.users.PinRefusal
import kz.mybrain.superkassa.desktop.ui.users.UserRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Правила заведения кассиров.
 *
 * Стандартные пины и последний носитель роли — те два случая, в которых
 * узел отказывает кодом, а администратор должен увидеть причину словами.
 */
class MoneyUserRulesTest {

    @Test
    fun `пустое поле пина ошибкой не считается`() {
        assertNull(UserRules.checkPin(""))
    }

    @Test
    fun `короткий и длинный пин узел не примет`() {
        assertEquals(PinRefusal.TooShort, UserRules.checkPin("12"))
        assertEquals(PinRefusal.TooLong, UserRules.checkPin("123456789"))
        assertNull(UserRules.checkPin("1234"))
        assertNull(UserRules.checkPin("12345678"))
    }

    @Test
    fun `стандартные пины запрещены`() {
        assertEquals(PinRefusal.Default, UserRules.checkPin("0000"))
        assertEquals(PinRefusal.Default, UserRules.checkPin("1111"))
    }

    @Test
    fun `в пин попадают только цифры и только восемь`() {
        assertEquals("12345678", UserRules.digitsOf("12ab34 567890"))
        assertEquals("4821", UserRules.digitsOf("48-21"))
    }

    @Test
    fun `кассир заводится с именем и годным пином`() {
        assertTrue(UserRules.canCreate("Айгүл", "4821"))
        assertFalse(UserRules.canCreate("  ", "4821"))
        assertFalse(UserRules.canCreate("Айгүл", "111"))
        assertFalse(UserRules.canCreate("Айгүл", "1111"))
    }

    @Test
    fun `последнего носителя роли удалять нельзя`() {
        val admin = KkmUser(userId = "1", name = "Администратор", role = "ADMIN")
        val cashier = KkmUser(userId = "2", name = "Кассир", role = "CASHIER")
        val second = KkmUser(userId = "3", name = "Второй кассир", role = "CASHIER")

        assertTrue(UserRules.lastOfRole(listOf(admin, cashier), admin))
        assertTrue(UserRules.lastOfRole(listOf(admin, cashier), cashier))
        assertFalse(UserRules.lastOfRole(listOf(admin, cashier, second), cashier))
        assertTrue(UserRules.lastOfRole(listOf(admin, cashier, second), admin))
    }
}
