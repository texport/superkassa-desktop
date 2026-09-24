package kz.mybrain.superkassa.domain.users.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Вход и заведение пина живут по одним пределам длины.
 *
 * Пинов по умолчанию у кассы нет: все пины задаёт пользователь, и пин
 * из одинаковых цифр — такой же пин, как любой другой. Запрет «0000»
 * и «1111» держался на стандартном пине кассы, которого больше нет.
 */
class LoginPinTest {

    @Test
    fun `пин из одинаковых цифр годится и для входа, и для заведения`() {
        assertTrue(UserRules.pinEnterable("0000"))
        assertTrue(UserRules.pinAccepted("1111"))
    }

    @Test
    fun `слишком короткий пин не годится ни для входа, ни для заведения`() {
        assertFalse(UserRules.pinEnterable("12"))
        assertFalse(UserRules.pinAccepted("12"))
    }

    @Test
    fun `слишком длинный пин не годится ни для входа, ни для заведения`() {
        assertFalse(UserRules.pinEnterable("12345678901"))
        assertFalse(UserRules.pinAccepted("12345678901"))
    }

    @Test
    fun `обычный пин годится и туда, и туда`() {
        assertTrue(UserRules.pinEnterable("4821"))
        assertTrue(UserRules.pinAccepted("4821"))
    }
}
