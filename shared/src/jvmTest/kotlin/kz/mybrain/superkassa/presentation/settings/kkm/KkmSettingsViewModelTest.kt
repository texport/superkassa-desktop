package kz.mybrain.superkassa.presentation.settings.kkm

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.MemoryChoices
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Сама касса в настройках без окна: название, режим программирования, снятие.
 *
 * Касса — фасад по заказу: отвечает кассой, отказывает кодом и словами,
 * падает сбоем. Итог объявляется только после согласия кассы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KkmSettingsViewModelTest {

    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val machine = MemoryChoices()
    private val app =
        CoreScene.app(core, signIn, notices, machine.memory, settings = settingsPorts().copy(workplace = machine))
    private val money = textsOf(Language.Ru).kassa.money.kkm
    private val texts = textsOf(Language.Ru).common.settings

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        signIn.enter(CoreScene.kkm(id = "kkm-1", name = null), CoreScene.cashier(), CoreScene.PIN)
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    @Test
    fun `название уходит в кассу, и касса зовётся им всюду`() {
        core.on("updateKkmName") { args -> CoreScene.kkm(id = args[0] as String, name = args[2] as String?) }
        val model = kkmSettingsModel(app)

        model.typeName("Касса 2 на Достык")
        model.saveName()

        assertEquals("Касса 2 на Достык", signIn.state.value.kkm?.name, "шапка и вход не узнали нового названия")
        assertEquals(Message.Done(money.renameSaved), notices.last)
        assertNull(machine.memory.names["kkm-1"], "своё название рабочего места перекрыло бы записанное в кассу")
        assertNull(model.state.value.nameDraft)
    }

    /** Касса не приняла пин: название остаётся хотя бы на этой машине, набранное — в поле. */
    @Test
    fun `отказ кассы оставляет название на рабочем месте`() {
        core.refuse("updateKkmName", "USER_NOT_FOUND", ru = "Пользователь не найден")
        val model = kkmSettingsModel(app)

        model.typeName("Вторая линия")
        model.saveName()

        assertEquals(Message.Refusal("Пользователь не найден", "USER_NOT_FOUND"), notices.last)
        assertEquals("Вторая линия", machine.memory.names["kkm-1"])
        assertEquals("Вторая линия", model.state.value.nameField, "набранное пропало после отказа")
    }

    @Test
    fun `вход в режим программирования виден сразу и объявлен`() {
        core.on("enterProgramming") { CoreScene.kkm(state = "PROGRAMMING") }
        val model = kkmSettingsModel(app)

        model.switchProgramming()

        assertTrue(model.state.value.programming)
        assertEquals(Message.Done(texts.enteredProgramming), notices.last)
        assertFalse(model.state.value.busy)
    }

    /** Пин заблокирован: касса говорит, сколько ждать, а режим не меняется. */
    @Test
    fun `заблокированный пин называется словами кассы`() {
        core.on("enterProgramming") { throw PinLockedException(retryAfterSeconds = 120) }
        val model = kkmSettingsModel(app)

        model.switchProgramming()

        assertEquals("PIN_LOCKED", (notices.last as Message.Refusal).code)
        assertFalse(model.state.value.programming)
    }

    /** Оставаться в настройках удалённой кассы нельзя: каждое обращение отвечало бы «не найдена». */
    @Test
    fun `снятая касса уводит на выбор кассы`() {
        core.on("deleteKkm") { true }
        val model = kkmSettingsModel(app)

        model.askDecommission()
        model.decommission()

        assertNull(signIn.state.value.kkm)
        assertEquals(Message.Done(money.decommissionDone), notices.last)
        assertFalse(model.state.value.decommissionAsked)
    }

    @Test
    fun `сбой кассы при снятии оставляет кассу на месте`() {
        core.on("deleteKkm") { error("database is locked") }
        val model = kkmSettingsModel(app)

        model.decommission()

        assertEquals("kkm-1", signIn.state.value.kkm?.kkmId)
        assertIs<Message.Failed>(notices.last)
    }

    /** Набранное для одной кассы в настройках другой не показывается. */
    @Test
    fun `смена кассы забывает набранное название`() {
        val model = kkmSettingsModel(app)
        model.typeName("Вторая линия")

        signIn.enter(CoreScene.kkm(id = "kkm-2", name = "Касса в зале"), CoreScene.cashier(), CoreScene.PIN)

        assertEquals("Касса в зале", model.state.value.nameField)
    }
}
