package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.presentation.login.LoginViewModel
import kz.mybrain.superkassa.presentation.messages.Message
import kz.mybrain.superkassa.presentation.messages.Notices
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Вход кассира без окна: модель, касса по заказу и держатель входа.
 *
 * Экран входа рисует только то, что здесь проверено: список касс, выбор,
 * пин и итог проверки пина кассой.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val memory = MemoryWorkplace()
    private val first = CoreScene.kkm(id = "kkm-1", kgd = "000000200042", name = "Касса у входа")
    private val second = CoreScene.kkm(id = "kkm-2", kgd = "000000200043", name = "Касса у окна")

    private fun model(): LoginViewModel =
        LoginViewModel(CoreScene.app(core, signIn, notices, memory))

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `список касс прочитан, и выбрана касса прошлого раза`() {
        core.on("listKkms") { listOf(first, second).let(CoreScene::page) }
        memory.rememberedKkmId = "kkm-2"
        val model = model()

        model.reload()

        val state = model.state.value
        assertTrue(state.answered && state.listRead)
        assertEquals(listOf("kkm-1", "kkm-2"), state.kkms.map { it.kkmId })
        assertEquals("kkm-2", state.chosen?.kkmId)
    }

    /** Касса, не отдавшая список, — не касса без касс: о них неизвестно ничего. */
    @Test
    fun `непрочитанный список не выдаётся за пустой`() {
        val model = model()

        model.reload()

        val state = model.state.value
        assertTrue(state.answered, "ответа кассы ждали бы вечно")
        assertFalse(state.listRead, "список, которого касса не отдала, объявлен прочитанным")
        assertIs<Message.Failed>(notices.last)
    }

    /** Набранный номер сильнее выбора мышью: он сделан позже. */
    @Test
    fun `набранный номер отменяет выбор мышью`() {
        core.on("listKkms") { listOf(first, second).let(CoreScene::page) }
        val model = model()
        model.reload()
        model.pick(first)

        model.search("200043")

        assertEquals("kkm-2", model.state.value.chosen?.kkmId)
    }

    @Test
    fun `пин набирается цифрами и не длиннее восьми`() {
        val model = model()

        model.typePin("12a34-5678 90")

        assertEquals("12345678", model.state.value.pin)
    }

    @Test
    fun `верный пин начинает работу, и касса запоминается`() {
        core.on("listKkms") { listOf(first).let(CoreScene::page) }
        core.on("authenticate") { CoreScene.cashier(admin = false) }
        val model = model()
        model.reload()
        model.typePin(CoreScene.PIN)

        model.enter()

        val now = signIn.state.value
        assertTrue(now.signedIn)
        assertEquals(UserRole.CASHIER, now.cashier?.role)
        assertFalse(now.isAdmin)
        assertEquals("kkm-1", memory.rememberedKkmId)
        assertEquals("", model.state.value.pin, "принятый пин остался на экране")
    }

    /** Отказ кассы — её словами на языке кассира; пин остаётся, чтобы его исправить. */
    @Test
    fun `неверный пин — отказ словами кассы, вход не начат`() {
        core.on("listKkms") { listOf(first).let(CoreScene::page) }
        core.refuse("authenticate", "INVALID_PIN", ru = "Неверный пин", kk = "Пин дұрыс емес", en = "Wrong pin")
        val model = model()
        model.reload()
        model.typePin("9999")

        model.enter()

        assertFalse(signIn.state.value.signedIn)
        assertEquals(Message.Refusal("Неверный пин", "INVALID_PIN"), notices.last)
        assertEquals("9999", model.state.value.pin)
        assertFalse(model.state.value.entering)
        assertNull(memory.rememberedKkmId)
    }

    @Test
    fun `короткий пин кассе не отправляется`() {
        core.on("listKkms") { listOf(first).let(CoreScene::page) }
        val model = model()
        model.reload()
        model.typePin("12")

        model.enter()

        assertFalse("authenticate" in core.calls)
    }
}
