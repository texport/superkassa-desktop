package kz.mybrain.superkassa.presentation.setup

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitSimpleRequest
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemorySetup
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Последний шаг пути через кабинет: идентификатор и название — из пройденного,
 * токен — от кабинета в миг заведения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupCabinetPathTest {
    private val core = FakeCore()
    private val memory = MemorySetup()
    private val cabinet = FakeSetupCabinet()
    private var done = 0

    @BeforeTest
    fun inlineMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val dev = OfdEnvironmentResponse("DEV", TrilingualMessageResponse("DEV", "DEV", "DEV"))
        core.on("getOfdEnvironments") { listOf(dev) }
        core.on("listKkms") { CoreScene.page(emptyList()) }
    }

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(): SetupViewModel =
        setupModel(CoreScene.app(core), SetupPorts(memory, cabinet), DirectCalls()).apply { reload() }

    private fun connect(model: SetupViewModel, token: String?) {
        cabinet.token = token
        model.connect { done++ }
    }

    @Test
    fun `через кабинет — идентификатор и название из пройденного, пройденное забыто`() {
        var named: List<Any?> = emptyList()
        var request: KkmInitSimpleRequest? = null
        core.on("initKkmSimple") { args ->
            request = args.single() as KkmInitSimpleRequest
            CoreScene.kkm(id = "kkm-9")
        }
        core.on("getKkm") { CoreScene.kkm(id = "kkm-9") }
        core.on("updateKkmName") { args -> CoreScene.kkm(id = "kkm-9").also { named = args } }
        val model = model()
        model.rememberRegister("r-1", 5_000_021, "Касса у входа")
        model.edit(KkmForm(adminPin = "4821", adminPinRepeat = "4821"))

        connect(model, token = "3735928559")

        assertEquals("5000021", request?.ofdSystemId)
        assertEquals("3735928559", request?.ofdToken)
        assertEquals(listOf<Any?>("kkm-9", "4821", "Касса у входа"), named, "касса не названа пином её администратора")
        assertNull(model.state.value.draft.cabinetRegisterId, "пройденное не забыто")
        assertNull(memory.setupValue("register"))
        assertEquals(1, done)
    }

    @Test
    fun `кабинет токена не выдал — касса не заводится`() {
        val model = model()
        model.rememberRegister("r-1", 5_000_021, null)
        model.edit(KkmForm(adminPin = "4821", adminPinRepeat = "4821"))

        connect(model, token = null)

        assertFalse("initKkmSimple" in core.calls)
        assertFalse(model.state.value.viaCabinet.busy)
    }
}
