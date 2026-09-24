package kz.mybrain.superkassa.presentation.users

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import io.github.texport.superkassa.core.presentation.api.model.user.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
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
 * Кассиры без окна: касса по заказу и держатель входа.
 *
 * Главное здесь — чей пин уходит кассе: сменивший пин себе продолжает
 * работу новым, сменивший его другому — своим прежним.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsersViewModelTest {
    private val scene = UsersScene()
    private val notices = Notices()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(): UsersViewModel = usersModel(CoreScene.services(scene.core, scene.signIn, notices))

    @Test
    fun `список прочитан пином работающего`() {
        val state = model().state.value

        assertEquals(listOf("u-1", "u-2"), state.users.map { it.userId })
        assertTrue(state.answered && !state.unreadable)
        assertEquals(listOf("listUsers ${CoreScene.PIN}"), scene.asked)
        assertTrue(state.own(UsersScene.ADMIN))
        assertFalse(state.deletable(UsersScene.ADMIN), "единственного администратора дают удалить")
        assertTrue(state.deletable(UsersScene.CASHIER))
    }

    /**
     * Модель окна переживает смену кассира: вошедший после администратора
     * кассир получал отказ «операция запрещена» за раздел, которого не видит.
     */
    @Test
    fun `за кассира список кассиров не спрашивается`() {
        model()
        scene.asked.clear()

        scene.signIn.enter(CoreScene.kkm(id = "kkm-2"), UsersScene.CASHIER, "4826")

        assertTrue(scene.asked.isEmpty(), "кассу спросили за кассира: ${scene.asked}")
        assertNull(notices.last)
    }

    @Test
    fun `отказ кассы — не пустая касса`() {
        scene.core.refuse("listUsers", "FORBIDDEN", ru = "Нет прав")
        val state = model().state.value

        assertTrue(state.unreadable)
        assertEquals(Message.Refusal("Нет прав", "FORBIDDEN"), notices.last)
    }

    @Test
    fun `сбой кассы — не пустая касса`() {
        scene.core.on("listUsers") { error("database is locked") }
        val state = model().state.value

        assertTrue(state.unreadable)
        assertIs<Message.Failed>(notices.last)
    }

    @Test
    fun `пин себе — работа продолжается новым пином`() {
        val model = model()
        model.askPin(UsersScene.ADMIN)
        model.typeNewPin("4821")

        model.confirmPin()

        assertEquals("4821", scene.signIn.state.value.pin)
        assertEquals("updateUser ${CoreScene.PIN}", scene.asked[1], "пин сменён не пином работающего")
        assertEquals("listUsers 4821", scene.asked.last(), "список не перечитан новым пином")
        assertNull(model.state.value.pin, "окно смены пина не закрылось")
        assertEquals(Message.Done("${UsersScene.ADMIN.name} — Пин изменён"), notices.last)
    }

    @Test
    fun `пин другому — работа остаётся под своим пином`() {
        val model = model()
        model.askPin(UsersScene.CASHIER)
        model.typeNewPin("5555")

        model.confirmPin()

        assertEquals(CoreScene.PIN, scene.signIn.state.value.pin, "работа ушла под пин кассира")
        assertFalse(scene.asked.any { it.endsWith(" 5555") }, "касса спрошена чужим пином: ${scene.asked}")
        assertEquals("listUsers ${CoreScene.PIN}", scene.asked.last())
    }

    @Test
    fun `пин занят — окно остаётся открытым и говорит почему`() {
        scene.core.refuse("updateUser", "USER_PIN_CONFLICT", ru = TAKEN)
        val model = model()
        model.askPin(UsersScene.CASHIER)
        model.typeNewPin("5555")

        model.confirmPin()

        val change = assertNotNull(model.state.value.pin, "окно закрылось на отказе")
        assertEquals(TAKEN, change.refusal)
        assertFalse(change.busy)
        assertEquals("5555", change.pin, "набранный пин стёрт")
        assertEquals(CoreScene.PIN, scene.signIn.state.value.pin)
    }

    @Test
    fun `касса заперта — окно говорит, сколько ждать`() {
        scene.core.on("updateUser") { throw PinLockedException(retryAfterSeconds = 90) }
        val model = model()
        model.askPin(UsersScene.ADMIN)
        model.typeNewPin("4821")

        model.confirmPin()

        val refusal = assertNotNull(model.state.value.pin?.refusal, "запертая касса не объяснена")
        assertTrue(refusal.contains("2"), "не сказано, сколько ждать: $refusal")
        assertEquals(CoreScene.PIN, scene.signIn.state.value.pin, "пин сменён без согласия кассы")
    }

    @Test
    fun `новый кассир заводится, форма очищается`() {
        var asked: UserCreateRequest? = null
        scene.core.on("createUser") { args -> UsersScene.CASHIER.also { asked = args[2] as UserCreateRequest } }
        val model = model()
        model.edit(CashierForm(name = "  Дана Жумабаева ", role = UserRole.CASHIER, pin = "48-21"))

        model.create()

        assertEquals(UserCreateRequest("Дана Жумабаева", UserRole.CASHIER, "4821"), asked)
        assertEquals(CashierForm(), model.state.value.form)
        assertEquals(Message.Done("Кассир заведён: Дана Жумабаева"), notices.last)
    }

    @Test
    fun `занятый пин при заведении — словами кассы, набранное остаётся`() {
        scene.core.refuse("createUser", "USER_PIN_CONFLICT", ru = TAKEN)
        val model = model()
        model.edit(CashierForm(name = "Дана", pin = "4821"))

        model.create()

        assertEquals(Message.Refusal(TAKEN, "USER_PIN_CONFLICT"), notices.last)
        assertEquals("4821", model.state.value.form.pin)
        assertFalse(model.state.value.form.busy)
    }

    @Test
    fun `удаливший себя выходит, удаливший другого остаётся`() {
        val model = model()
        model.askRemove(UsersScene.CASHIER)
        model.confirmRemove()
        assertTrue(scene.signIn.state.value.signedIn, "удаление кассира вывело администратора")

        model.askRemove(UsersScene.ADMIN)
        model.confirmRemove()
        assertFalse(scene.signIn.state.value.signedIn, "удалённый кассир остался за кассой")
        assertNull(model.state.value.removing)
    }

    private companion object {
        /** Слова кассы о занятом пине. */
        const val TAKEN = "Такой пин на этой кассе уже занят. Задайте другой."
    }
}
