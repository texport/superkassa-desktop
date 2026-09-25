package kz.mybrain.superkassa.presentation.setup

import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.setup.model.SetupWay
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.kassa.services
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Мастер там, где кабинета нет, — на Android: только ручной путь.
 *
 * Путь через кабинет там выбрать нечем, и мастер, начавший с него, заводил
 * бы кассу по пустому идентификатору из пройденного, а не по набранному.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupWithoutCabinetTest {
    private val core = FakeCore()

    @BeforeTest
    fun inlineMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("getOfdEnvironments") { CONTOURS }
        core.on("listKkms") { CoreScene.page(emptyList()) }
    }

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `без кабинета мастер начинает с ручного пути`() {
        val model = setupModel(CoreScene.services(core), SetupPorts(MemorySetup()), WithoutCabinet)

        assertEquals(SetupWay.ByHand, model.state.value.way)
    }

    @Test
    fun `без кабинета касса заводится набранными идентификатором и токеном`() {
        var asked: List<Any?> = emptyList()
        core.on("initKkmSimple") { args -> CoreScene.kkm(id = "kkm-9").also { asked = args } }
        core.on("getKkm") { CoreScene.kkm(id = "kkm-9") }
        val model = setupModel(CoreScene.services(core), SetupPorts(MemorySetup()), WithoutCabinet).apply { reload() }
        model.edit(KkmForm(systemId = "5000021", token = "3735928559", adminPin = "4821", adminPinRepeat = "4821"))

        var done = false
        model.connect { done = true }

        assertTrue(done, "касса не заведена")
        assertTrue(asked.toString().contains("5000021"), "завели не набранную кассу: $asked")
    }

    private companion object {
        val CONTOURS = listOf("TEST", "DEV", "PROD")
            .map { OfdEnvironmentResponse(it, TrilingualMessageResponse(it, it, it)) }
    }
}
