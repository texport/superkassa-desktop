package kz.mybrain.superkassa.presentation.setup

import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.setup.model.SetupWay
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Заведение кассы мастером без окна: касса по заказу и пройденное в памяти.
 *
 * Главное здесь — что мастер не называет кассу подключённой, пока она
 * не читается. Касса процесса при молчащей БФД отвечает на заведение
 * успехом и сведениями о кассе, которой в её базе нет.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {
    private val core = FakeCore()
    private val notices = Notices()
    private val memory = MemorySetup()
    private val texts = textsOf(Language.Ru).setup
    private var done = 0
    private val cabinet = FakeSetupCabinet(token = "3735928559")
    private val workplace = MemoryWorkplace()

    @BeforeTest
    fun inlineMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("getOfdEnvironments") { CONTOURS }
        core.on("listKkms") { CoreScene.page(emptyList()) }
    }

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(): SetupViewModel =
        setupModel(
            CoreScene.services(core, notices = notices, memory = workplace),
            SetupPorts(memory, cabinet),
            DirectCalls()
        )
            .apply { reload() }

    private fun byHand(model: SetupViewModel) {
        model.chooseWay(SetupWay.ByHand)
        model.edit(KkmForm(systemId = "5000021", token = "3735928559", adminPin = "4821", adminPinRepeat = "4821"))
    }

    private fun connect(model: SetupViewModel) = model.connect { done++ }

    @Test
    fun `вручную касса заводится и объявляется, только когда читается`() {
        var asked: List<Any?> = emptyList()
        core.on("initKkmSimple") { args -> CoreScene.kkm(id = "kkm-9").also { asked = args } }
        core.on("getKkm") { CoreScene.kkm(id = "kkm-9") }
        val model = model().also(::byHand)

        connect(model)

        // Пин администратора — только в запросе: отдельного пина заведения нет.
        val request = asked.single() as KkmInitSimpleRequest
        assertEquals("BFD", request.ofdId, "поставщик не подставлен")
        assertEquals("DEV", request.ofdEnvironment, "контур не подставлен первым поднятым")
        assertEquals("5000021", request.ofdSystemId)
        assertEquals("4821", request.adminPin)
        assertEquals(Message.Done(texts.connected), notices.last)
        assertEquals(1, done)
        assertEquals(KkmForm(), model.state.value.byHand, "набранный токен остался на экране")
        // Вход открывается на заведённой кассе, а не на той, что была раньше.
        assertEquals("kkm-9", workplace.rememberedKkmId, "заведённая касса не стала кассой рабочего места")
    }

    /** Дефект ядра: при недоступной БФД `initKkm` отвечает кассой, которой нет в базе. */
    @Test
    fun `касса, которой нет в базе, подключённой не называется`() {
        core.on("initKkmSimple") { CoreScene.kkm(id = "kkm-ghost") }
        core.on("getKkm") { throw NotFoundException(TrilingualMessage("нет", "жоқ", "none"), "KKM_NOT_FOUND") }
        val model = model().also(::byHand)

        connect(model)

        val shown = assertIs<Message.Refusal>(notices.last)
        assertEquals(texts.notStored, shown.text)
        assertEquals(0, done, "мастер закрылся над незаведённой кассой")
        assertEquals("5000021", model.state.value.byHand.systemId, "набранное стёрто, повторять нечем")
        assertFalse(model.state.value.byHand.busy)
    }

    @Test
    fun `отказ кассы — её словами`() {
        core.refuse("initKkmSimple", "KKM_SYSTEM_ID_EXISTS", ru = "Касса с таким идентификатором уже есть")
        val model = model().also(::byHand)

        connect(model)

        assertEquals(Message.Refusal("Касса с таким идентификатором уже есть", "KKM_SYSTEM_ID_EXISTS"), notices.last)
        assertFalse("getKkm" in core.calls)
        assertEquals(0, done)
    }

    @Test
    fun `сбой кассы назван сбоем`() {
        core.on("initKkmSimple") { error("disk full") }
        val model = model().also(::byHand)

        connect(model)

        assertIs<Message.Failed>(notices.last)
        assertEquals(0, done)
    }

    /** Стандартного пина нет: пин администратора набирается дважды, и расхождение не заводит кассу. */
    @Test
    fun `несовпавший повтор пина кассу не заводит`() {
        val model = model()
        model.chooseWay(SetupWay.ByHand)
        model.edit(KkmForm(systemId = "5000021", token = "3735928559", adminPin = "4821", adminPinRepeat = "4812"))

        assertTrue(model.state.value.byHand.pinsDiffer)
        connect(model)

        assertFalse("initKkmSimple" in core.calls, "касса заведена с неподтверждённым пином")
    }

    /** Длина пина — то же правило, что у кассы: от 4 до 10 цифр; лишнее не набирается. */
    @Test
    fun `пин администратора от четырёх до десяти цифр`() {
        val model = model()
        model.chooseWay(SetupWay.ByHand)
        model.edit(KkmForm(systemId = "5000021", token = "1", adminPin = "482", adminPinRepeat = "482"))
        assertFalse(model.state.value.ready, "касса заводится с пином из трёх цифр")

        model.edit(KkmForm(systemId = "5000021", token = "1", adminPin = "123456789012", adminPinRepeat = "1234567890"))
        assertEquals("1234567890", model.state.value.byHand.adminPin)
        assertTrue(model.state.value.ready)
    }

    /** Касса контуров не назвала: заводить некуда, и кнопка не оживает от одного пина. */
    @Test
    fun `без контура касса не заводится`() {
        core.refuse("getOfdEnvironments", "INTERNAL")
        val model = model()
        model.rememberRegister("r-1", 5_000_021, null)
        model.edit(KkmForm(adminPin = "4821", adminPinRepeat = "4821"))

        assertFalse(model.state.value.ready)
        connect(model)

        assertFalse("initKkmSimple" in core.calls, "касса заведена без контура: ${core.calls}")
    }

    /** Пройденным последний шаг считается по кассе с идентификатором из пройденного, а не по любой. */
    @Test
    fun `последний шаг пройден только своей кассой`() {
        core.on("listKkms") { CoreScene.page(listOf(CoreScene.kkm().copy(ofdSystemId = null))) }
        val alone = model()
        assertFalse(alone.state.value.connected, "чужая касса без сведений о БФД пометила шаг пройденным")

        core.on("listKkms") { CoreScene.page(listOf(CoreScene.kkm().copy(ofdSystemId = "5000021"))) }
        val own = model()
        own.rememberRegister("r-1", 5_000_021, null)
        own.reload()
        assertTrue(own.state.value.connected)
    }

    private companion object {
        val CONTOURS = listOf("TEST", "DEV", "PROD")
            .map { OfdEnvironmentResponse(it, TrilingualMessageResponse(it, it, it)) }
    }
}
