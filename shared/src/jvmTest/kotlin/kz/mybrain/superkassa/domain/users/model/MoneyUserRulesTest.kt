package kz.mybrain.superkassa.domain.users.model

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Правила заведения кассиров.
 *
 * Длина пина и последний носитель роли — те случаи, в которых касса
 * отказывает кодом, а администратор должен увидеть причину словами.
 */
class MoneyUserRulesTest {

    @Test
    fun `пустое поле пина ошибкой не считается`() {
        assertNull(UserRules.checkPin(""))
    }

    @Test
    fun `короткий и длинный пин узел не примет`() {
        assertEquals(PinRefusal.TooShort, UserRules.checkPin("12"))
        assertEquals(PinRefusal.TooLong, UserRules.checkPin("12345678901"))
        assertNull(UserRules.checkPin("1234"))
        assertNull(UserRules.checkPin("1234567890"))
    }

    /** Пинов по умолчанию у кассы нет, и особых пинов тоже: годен любой пин годной длины. */
    @Test
    fun `пин из одинаковых цифр — обычный пин`() {
        assertNull(UserRules.checkPin("0000"))
        assertNull(UserRules.checkPin("1111"))
        assertTrue(UserRules.pinAccepted("0000"))
    }

    @Test
    fun `в пин попадают только цифры и только восемь`() {
        assertEquals("1234567890", UserRules.digitsOf("12ab34 5678901"))
        assertEquals("4821", UserRules.digitsOf("48-21"))
    }

    @Test
    fun `кассир заводится с именем и годным пином`() {
        assertTrue(UserRules.canCreate("Айгүл", "4821"))
        assertFalse(UserRules.canCreate("  ", "4821"))
        assertFalse(UserRules.canCreate("Айгүл", "111"))
        assertFalse(UserRules.canCreate("Айгүл", "12345678901"))
        assertTrue(UserRules.canCreate("Айгүл", "1111"), "пин из одинаковых цифр не годен, хотя пинов по умолчанию нет")
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
        val admin = UserResponse(userId = "1", name = "Администратор", role = UserRole.ADMIN)
        val cashier = UserResponse(userId = "2", name = "Кассир", role = UserRole.CASHIER)
        val second = UserResponse(userId = "3", name = "Второй кассир", role = UserRole.CASHIER)
        val deputy = UserResponse(userId = "4", name = "Второй администратор", role = UserRole.ADMIN)

        assertTrue(UserRules.lastAdmin(listOf(admin, cashier), admin))
        assertFalse(UserRules.lastAdmin(listOf(admin, cashier), cashier))
        assertFalse(UserRules.lastAdmin(listOf(admin, cashier, second), cashier))
        assertFalse(UserRules.lastAdmin(listOf(admin, deputy, cashier), admin))
    }

    /** Своего кассира узнают по опознавателю кассы, а не по имени. */
    @Test
    fun `тёзки — разные кассиры`() {
        val one = UserResponse(userId = "1", name = "Дана Жумабаева", role = UserRole.CASHIER)
        val other = UserResponse(userId = "2", name = "Дана Жумабаева", role = UserRole.CASHIER)
        val nameless = UserResponse(userId = "", name = "Без опознавателя", role = UserRole.CASHIER)

        assertTrue(UserRules.same(one, one))
        assertFalse(UserRules.same(one, other))
        assertFalse(UserRules.same(nameless, nameless))
        assertFalse(UserRules.same(null, one))
    }
}
