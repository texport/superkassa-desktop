package kz.mybrain.superkassa.presentation.settings.core

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.MemoryCoreSettings
import kz.mybrain.superkassa.presentation.settings.settingsPorts
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
 * Настройки кассы на этой машине без окна: чтение, правка и закрытая правка.
 *
 * Закрытая правка видна до нажатия; отказ, пришедший всё же, называется
 * словами для владельца, а не словами кассы про «API».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoreSettingsViewModelTest {

    private val notices = Notices()
    private val texts = textsOf(Language.Ru).settings.core

    @BeforeTest
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    private fun model(store: MemoryCoreSettings): CoreSettingsViewModel {
        val settings = settingsPorts().copy(coreSettings = store)
        return coreSettingsModel(CoreScene.app(FakeCore(), notices = notices, settings = settings))
    }

    @Test
    fun `ожидание ответа БФД сохраняется и объявлено с оговоркой о перезапуске`() {
        val store = MemoryCoreSettings()
        val model = model(store)
        assertEquals("7", model.state.value.timeout, "действующее ожидание не прочитано")

        model.typeTimeout("12")
        model.save()

        assertEquals(12L, store.settings.ofdTimeoutSeconds)
        assertEquals(Message.Done(texts.saved), notices.last)
        assertEquals("12", model.state.value.timeout)
    }

    @Test
    fun `негодное число секунд не сохраняется`() {
        val model = model(MemoryCoreSettings())

        model.typeReconnect("0")
        assertFalse(model.state.value.savable)
        model.typeReconnect("минута")
        assertFalse(model.state.value.reconnectValid)
    }

    /** Владелец закрыл правку: это видно до нажатия, а кнопка погашена. */
    @Test
    fun `закрытая правка видна до нажатия`() {
        val store = MemoryCoreSettings(CoreSettings(CoreMode.DESKTOP, StorageSettings("SQLITE", "jdbc:sqlite:x.db")))
        val model = model(store)

        model.typeTimeout("12")

        assertTrue(model.state.value.frozen)
        assertFalse(model.state.value.savable)
        model.save()
        assertEquals(0, store.saves)
    }

    /** Правку закрыли, пока карточка была открыта: отказ — словами для владельца. */
    @Test
    fun `отказ закрытой правки назван словами для владельца`() {
        val store = MemoryCoreSettings()
        val model = model(store)
        store.settings = store.settings.copy(allowChanges = false)

        model.typeTimeout("12")
        model.save()

        assertEquals(Message.Refusal(texts.frozenHint, "SETTINGS_FROZEN"), notices.last)
    }

    @Test
    fun `касса не отдала настройки — сбой назван, полей нет`() {
        val store = object : MemoryCoreSettings() {
            override suspend fun read(): CoreSettings = error("settings file is broken")
        }
        val model = model(store)

        assertIs<Message.Failed>(notices.last)
        assertEquals(null, model.state.value.settings)
    }
}
