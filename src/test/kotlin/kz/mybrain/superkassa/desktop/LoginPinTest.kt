package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.users.UserRules
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Вход и заведение пина живут по разным правилам.
 *
 * Только что заведённая касса работает под стандартным пином
 * администратора, и запрет на вход с ним означал бы, что открыть такую
 * кассу нельзя вовсе — в том числе чтобы сменить пин, как велит подсказка
 * сразу после заведения.
 */
class LoginPinTest {

    @Test
    fun `со стандартным пином войти можно`() {
        assertTrue(UserRules.pinEnterable("0000"))
        assertTrue(UserRules.pinEnterable("1111"))
    }

    @Test
    fun `стандартный пин нельзя назначить`() {
        assertFalse(UserRules.pinAccepted("0000"))
        assertFalse(UserRules.pinAccepted("1111"))
    }

    @Test
    fun `слишком короткий пин не годится ни для входа, ни для заведения`() {
        assertFalse(UserRules.pinEnterable("12"))
        assertFalse(UserRules.pinAccepted("12"))
    }

    @Test
    fun `обычный пин годится и туда, и туда`() {
        assertTrue(UserRules.pinEnterable("4821"))
        assertTrue(UserRules.pinAccepted("4821"))
    }
}
