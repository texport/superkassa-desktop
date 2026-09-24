package kz.mybrain.superkassa.presentation.users

import io.github.texport.superkassa.core.presentation.api.model.user.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.user.UserResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Кассиры на настоящем ядре и тестовом БФД — как у администратора: действия
 * через модель экрана, итог — в самой кассе, отказы — её кодом и словами.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsersCoreTest {
    private val directory: File = createTempDirectory("kassa-users-").toFile()
    private val bench = appBench(directory)
    private val kassa: ReadyKassa = bench.registerKassa(appKassa(adminPin = ADMIN, cashierPin = CASHIER))
    private val notices = Notices()
    private val signIn = SignIn()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        Dispatchers.resetMain()
        bench.close()
        directory.deleteRecursively()
    }

    /** Экран кассиров под работающим с пином [pin]. */
    private fun model(pin: String = ADMIN): UsersViewModel {
        signIn.enter(kassa.info(), bench.api.authenticate(kassa.kkmId, pin), pin)
        return usersModel(CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, notices))
            .also { it.reload() }
    }

    private fun UsersViewModel.create(name: String, pin: String, role: UserRole = UserRole.CASHIER) {
        edit(CashierForm(name = name, role = role, pin = pin))
        create()
    }

    private fun inCore(name: String): UserResponse? =
        bench.api.listUsers(kassa.kkmId, ADMIN).firstOrNull { it.name == name }

    private fun refusal(): Message.Refusal = assertIs<Message.Refusal>(notices.last, "отказа нет: ${notices.last}")

    @Test
    fun `кассир заводится с пином от четырёх до десяти цифр и входит им`() {
        val model = model()
        model.create("Дана", "5810")
        model.create("Ерлан", "1234567890")

        assertEquals(UserRole.CASHIER, bench.api.authenticate(kassa.kkmId, "5810").role)
        assertEquals("Ерлан", bench.api.authenticate(kassa.kkmId, "1234567890").name)
        assertTrue(model.state.value.users.map { it.name }.containsAll(listOf("Дана", "Ерлан")))
        assertIs<Message.Done>(notices.last)
    }

    @Test
    fun `пин короче четырёх не отправляется, длиннее десяти не набирается`() {
        val model = model()
        model.edit(CashierForm(name = "Дана", pin = "581"))
        assertFalse(model.state.value.canCreate, "три цифры приняты")

        model.edit(CashierForm(name = "Дана", pin = "12345678901"))
        assertEquals("1234567890", model.state.value.form.pin, "набрано больше десяти цифр")
    }

    @Test
    fun `занятый пин — отказ кассы её словами, набранное остаётся`() {
        val model = model()
        model.create("Дана", CASHIER)

        assertEquals("USER_PIN_CONFLICT", refusal().code)
        assertTrue(refusal().text.isNotBlank() && "USER_PIN" !in refusal().text)
        assertEquals("Дана", model.state.value.form.name, "набранное стёрто отказом")
        assertNull(inCore("Дана"))
    }

    @Test
    fun `пин чужому — он входит новым, администратор работает прежним`() {
        val model = model()
        val cashier = model.state.value.users.first { it.role == UserRole.CASHIER }

        model.askPin(cashier)
        model.typeNewPin("6402")
        model.confirmPin()

        assertEquals(cashier.userId, bench.api.authenticate(kassa.kkmId, "6402").userId)
        assertEquals(ADMIN, signIn.seat()?.pin, "пин работающего подменён чужим")
        assertNull(model.state.value.pin, "окно смены пина не закрылось")
    }

    @Test
    fun `пин себе — работа продолжается новым`() {
        val model = model()
        val me = checkNotNull(model.state.value.me)

        model.askPin(me)
        model.typeNewPin("9170")
        model.confirmPin()

        assertEquals("9170", signIn.seat()?.pin)
        model.reload()
        assertFalse(model.state.value.unreadable, "новым пином список не читается")
    }

    @Test
    fun `чужой занятый пин — отказ в окне смены, окно открыто`() {
        val model = model()
        val cashier = model.state.value.users.first { it.role == UserRole.CASHIER }

        model.askPin(cashier)
        model.typeNewPin(ADMIN)
        model.confirmPin()

        val change = assertNotNull(model.state.value.pin, "окно закрылось на отказе")
        assertTrue(!change.refusal.isNullOrBlank(), "причина отказа не показана в окне")
        assertEquals(cashier.userId, bench.api.authenticate(kassa.kkmId, CASHIER).userId, "прежний пин утрачен")
    }

    @Test
    fun `удаливший себя выходит из кассы`() {
        bench.api.createUser(kassa.kkmId, ADMIN, UserCreateRequest("Второй администратор", UserRole.ADMIN, SECOND))
        val model = model(SECOND)
        val me = checkNotNull(model.state.value.me)

        model.askRemove(me)
        model.confirmRemove()

        assertFalse(signIn.state.value.signedIn, "удаливший себя остался за кассой")
        assertNull(inCore("Второй администратор"))
    }

    @Test
    fun `касса заблокирована неверными пинами — список не читается и сказано почему`() {
        val model = model()
        repeat(WRONG_PINS) { runCatching { bench.api.authenticate(kassa.kkmId, "0000") } }

        model.reload()

        assertEquals("PIN_LOCKED", refusal().code)
        assertTrue(model.state.value.unreadable, "заблокированная касса выдана за пустой список")
    }

    private companion object {
        const val ADMIN = "7391"
        const val CASHIER = "4826"
        const val SECOND = "3157"

        /** Неверных пинов до блокировки у ядра — пять. */
        const val WRONG_PINS = 5
    }
}
