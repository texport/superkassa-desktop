package kz.mybrain.superkassa.presentation.shell.frame

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Каркас окна без окна: вход или рабочее окно и что стоит в шапке.
 *
 * Каркас ничего не выбирает сам — касса и кассир у него со слов держателя
 * входа, — а название кассы то, каким её назвали на этом рабочем месте.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {
    private val signIn = SignIn()
    private val notices = Notices()
    private val memory = MemoryWorkplace()

    private fun model(core: FakeCore = FakeCore()) = shellModel(CoreScene.app(core, signIn, notices, memory))

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `до входа окно — дверь, после входа — рабочее окно с кассой и кассиром`() {
        val model = model()
        assertFalse(model.state.value.seat.signedIn)
        assertNull(model.state.value.cashier)

        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)

        val shell = model.state.value
        assertTrue(shell.seat.signedIn)
        assertEquals("Касса у входа", shell.kkmName)
        assertEquals(CoreScene.cashier().name, shell.cashier)
    }

    /** Вход, сделанный до окна, окно застаёт сразу: экран входа поверх кассира не рисуется. */
    @Test
    fun `вход до окна виден с первого состояния`() {
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)

        assertTrue(model().state.value.seat.signedIn)
    }

    @Test
    fun `своё название рабочего места сильнее названия владельца`() {
        memory.names["kkm-1"] = "Касса у окна"
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)

        assertEquals("Касса у окна", model().state.value.kkmName)
    }

    @Test
    fun `кассир ушёл — касса осталась в шапке, кассира нет`() {
        val model = model()
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)

        signIn.signOut()

        assertFalse(model.state.value.seat.signedIn)
        assertEquals("Касса у входа", model.state.value.kkmName)
        assertNull(model.state.value.cashier)
    }

    /** Касса, перечитанная кнопкой шапки, видна всем разделам — и шапке тоже. */
    @Test
    fun `обновить перечитывает кассу`() {
        val core = FakeCore().apply { on("getKkm") { CoreScene.kkm(state = "BLOCKED") } }
        val model = model(core)
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)

        model.refresh()

        assertEquals("BLOCKED", model.state.value.kkm?.state)
        assertEquals("BLOCKED", signIn.state.value.kkm?.state, "перечитанную кассу не видят разделы")
        assertFalse(model.state.value.busy, "занятость не снята")
    }

    @Test
    fun `переход в другой раздел снимает итог прежнего действия`() {
        val model = model()
        notices.show(Message.Refusal("Смена не открыта", "SHIFT_NOT_OPEN"))

        model.sectionPicked()

        assertNull(notices.last)
    }
}
