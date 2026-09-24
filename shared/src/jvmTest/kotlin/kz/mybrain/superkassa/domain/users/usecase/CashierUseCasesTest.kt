package kz.mybrain.superkassa.domain.users.usecase

import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Сценарии кассиров: чей пин становится пином работающего и кто выходит. */
class CashierUseCasesTest {
    private val core = FakeCore().apply {
        on("updateUser") { CoreScene.cashier() }
        on("deleteUser") { true }
    }
    private val signIn = SignIn().apply { enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN) }
    private val kassa = core.kassa()
    private val other = UserResponse(userId = "u-2", name = "Дана Жумабаева", role = UserRole.CASHIER)

    @Test
    fun `пин себе — работа продолжается новым`(): Unit = runBlocking {
        assertEquals(Answer.Done(true), ChangeCashierPin(kassa, signIn)(CoreScene.cashier(), "4821"))
        assertEquals("4821", signIn.state.value.pin)
    }

    @Test
    fun `пин другому — работа остаётся под своим`(): Unit = runBlocking {
        assertEquals(Answer.Done(false), ChangeCashierPin(kassa, signIn)(other, "5555"))
        assertEquals(CoreScene.PIN, signIn.state.value.pin)
    }

    @Test
    fun `отказ кассы в смене пина пин работающего не трогает`(): Unit = runBlocking {
        core.refuse("updateUser", "USER_PIN_CONFLICT")
        ChangeCashierPin(kassa, signIn)(CoreScene.cashier(), "4821")
        assertEquals(CoreScene.PIN, signIn.state.value.pin)
    }

    /** Негодный пин и пустое имя до кассы не доходят: отказать она могла бы только кодом. */
    @Test
    fun `негодный пин кассе не отправляется`(): Unit = runBlocking {
        assertNull(CreateCashier(kassa, signIn)("Дана", UserRole.CASHIER, "12"))
        assertNull(CreateCashier(kassa, signIn)(" ", UserRole.CASHIER, "4821"))
        assertNull(ChangeCashierPin(kassa, signIn)(other, "12345678901"))
        assertTrue("createUser" !in core.calls && "updateUser" !in core.calls, "касса спрошена: ${core.calls}")
    }

    @Test
    fun `удаливший себя выходит, удаливший другого остаётся`(): Unit = runBlocking {
        assertEquals(Answer.Done(false), RemoveCashier(kassa, signIn)(other))
        assertTrue(signIn.state.value.signedIn)

        assertEquals(Answer.Done(true), RemoveCashier(kassa, signIn)(CoreScene.cashier()))
        assertFalse(signIn.state.value.signedIn)
    }
}
