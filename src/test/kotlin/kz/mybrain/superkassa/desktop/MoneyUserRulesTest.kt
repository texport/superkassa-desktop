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

    /**
     * Держится только администратор.
     *
     * Правило прежде запрещало удалять последнего носителя любой роли,
     * и администратор, расставшийся с единственным кассиром, упирался
     * в погашенную корзину и совет завести второго кассира. Касса без
     * кассиров — обычное её состояние: ровно такой она выходит
     * из мастера подключения.
     */
    @Test
    fun `удалять нельзя только единственного администратора`() {
        val admin = KkmUser(userId = "1", name = "Администратор", role = "ADMIN")
        val cashier = KkmUser(userId = "2", name = "Кассир", role = "CASHIER")
        val second = KkmUser(userId = "3", name = "Второй кассир", role = "CASHIER")
        val deputy = KkmUser(userId = "4", name = "Второй администратор", role = "ADMIN")

        assertTrue(UserRules.lastAdmin(listOf(admin, cashier), admin))
        assertFalse(UserRules.lastAdmin(listOf(admin, cashier), cashier))
        assertFalse(UserRules.lastAdmin(listOf(admin, cashier, second), cashier))
        assertFalse(UserRules.lastAdmin(listOf(admin, deputy, cashier), admin))
    }

    /** Своего кассира узнают по опознавателю узла, а не по имени. */
    @Test
    fun `тёзки — разные кассиры`() {
        val one = KkmUser(userId = "1", name = "Дана Жумабаева", role = "CASHIER")
        val other = KkmUser(userId = "2", name = "Дана Жумабаева", role = "CASHIER")
        val nameless = KkmUser(name = "Без опознавателя", role = "CASHIER")

        assertTrue(UserRules.same(one, one))
        assertFalse(UserRules.same(one, other))
        assertFalse(UserRules.same(nameless, nameless))
        assertFalse(UserRules.same(null, one))
    }
}
