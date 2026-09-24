package kz.mybrain.superkassa.presentation.setup

import io.github.texport.superkassa.core.presentation.api.model.common.FactoryNumberResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.local.Preferences
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Незаконченное подключение кассы переживает закрытие приложения.
 *
 * Проверка не про удобство: заводской номер касса выдаёт новым на каждый
 * запрос, и потерянное пройденное означает, что в кабинет унесли один
 * номер, а кассу завели с другим. Пройденное пишется на диск рабочего
 * места, и новая модель мастера — то же, что перезапуск приложения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KkmSetupDraftTest {
    private val core = FakeCore().apply {
        on("generateFactoryInfo") { FactoryNumberResponse("KZT26088C012846", 2026) }
    }

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun preferences(): Preferences {
        val home = Files.createTempDirectory("superkassa-draft").toFile()
        return Preferences(File(home, "settings"))
    }

    private fun wizard(preferences: Preferences) =
        setupModel(CoreScene.app(core), SetupPorts(preferences, FakeSetupCabinet()), DirectCalls())

    @Test
    fun `номер и год переживают перезапуск`() {
        val preferences = preferences()
        wizard(preferences).getFactory()

        val reopened = wizard(preferences).state.value.draft
        assertEquals("KZT26088C012846", reopened.factoryNumber)
        assertEquals("2026", reopened.manufactureYear)
    }

    @Test
    fun `второй раз номер не выдаётся`() {
        val preferences = preferences()
        val model = wizard(preferences)
        model.getFactory()
        model.getFactory()
        wizard(preferences).getFactory()

        assertEquals(1, core.calls.count { it == "generateFactoryInfo" }, "номер выдан заново: ${core.calls}")
    }

    @Test
    fun `мастер открывается там, где его оставили`() {
        val preferences = preferences()
        val model = wizard(preferences)
        assertEquals(SetupStep.Factory, model.state.value.draft.step)

        model.getFactory()
        assertEquals(SetupStep.Cabinet, wizard(preferences).state.value.draft.step)

        model.rememberRegister("22222222-2222-2222-2222-222222222222", 5000004, null)
        assertEquals(SetupStep.Application, wizard(preferences).state.value.draft.step)
    }

    @Test
    fun `идентификатор БФД запоминается вместе с кассой кабинета`() {
        val preferences = preferences()
        wizard(preferences).rememberRegister("33333333-3333-3333-3333-333333333333", 5000004, "Касса у входа")
        val reopened = wizard(preferences).state.value.draft
        assertEquals("5000004", reopened.systemId)
        assertEquals("Касса у входа", reopened.name)
    }

    @Test
    fun `начатое заново забывается и на диске`() {
        val preferences = preferences()
        val model = wizard(preferences)
        model.getFactory()
        model.rememberRegister("44444444-4444-4444-4444-444444444444", 5000004, null)

        model.startOver()

        val reopened = wizard(preferences).state.value.draft
        assertNull(reopened.factoryNumber)
        assertNull(reopened.cabinetRegisterId)
        assertNull(reopened.systemId)
        assertEquals(SetupStep.Factory, reopened.step)
    }

    @Test
    fun `начать заново спрашивают только о пройденном`() {
        val model = wizard(preferences())
        model.askStartOver(true)
        assertFalse(model.state.value.startingOver, "стирать нечего, а вопрос задан")

        model.rememberRegister("r-1", 5_000_021, null)
        model.askStartOver(true)
        assertFalse(model.state.value.startingOver, "без заводского номера мастер ещё не начат")

        model.getFactory()
        model.askStartOver(true)
        assertTrue(model.state.value.startingOver, "о начатом не спросили")
    }
}
