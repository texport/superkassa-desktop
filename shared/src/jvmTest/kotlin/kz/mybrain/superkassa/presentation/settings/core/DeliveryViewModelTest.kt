package kz.mybrain.superkassa.presentation.settings.core

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.domain.api.model.settings.TelegramProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.domain.settings.model.DeliveryRules
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
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
import kotlin.test.assertTrue

/**
 * Каналы доставки без окна: закрытая правка видна до нажатия, и отказ,
 * пришедший всё же, назван словами для владельца.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryViewModelTest {
    private val notices = Notices()
    private val texts = textsOf(Language.Ru).settings.core

    @BeforeTest
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    private fun model(store: MemoryCoreSettings): DeliveryViewModel {
        val settings = settingsPorts().copy(coreSettings = store)
        return deliveryModel(CoreScene.app(FakeCore(), notices = notices, settings = settings))
    }

    private fun settings(allowChanges: Boolean = true, mode: CoreMode = CoreMode.DESKTOP) = CoreSettings(
        mode = mode,
        storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:superkassa.db"),
        allowChanges = allowChanges,
        delivery = DeliverySettings(telegram = TelegramProviderSettings(botToken = "bot-key"))
    )

    @Test
    fun `правка, закрытая владельцем, видна до нажатия`() {
        val model = model(MemoryCoreSettings(settings(allowChanges = false)))

        model.type(DeliveryField.SmsUrl, "https://sms.example.kz")

        assertTrue(model.state.value.frozen)
        assertFalse(model.state.value.savable, "кнопка горит при закрытой правке")
    }

    @Test
    fun `отказ закрытой правкой назван словами владельца, а не кассы`() {
        val store = object : MemoryCoreSettings(settings()) {
            override suspend fun save(settings: CoreSettings): CoreSettings {
                this.settings = this.settings.copy(allowChanges = false)
                return super.save(settings)
            }
        }
        val model = model(store)

        model.type(DeliveryField.SmsUrl, "https://sms.example.kz")
        model.save()

        assertEquals(Message.Refusal(texts.frozenHint, "SETTINGS_FROZEN"), notices.last)
    }

    @Test
    fun `в режиме сервера причина — сервер`() {
        val model = model(MemoryCoreSettings(settings(mode = CoreMode.SERVER)))

        assertTrue(model.state.value.frozen && model.state.value.server)
    }

    @Test
    fun `заданный ключ стоит в поле знаком`() {
        val model = model(MemoryCoreSettings(settings()))

        assertEquals(DeliveryRules.HIDDEN, model.state.value.value(DeliveryField.TelegramToken))
        assertTrue(model.state.value.hidden(DeliveryField.TelegramToken))
        assertEquals("", model.state.value.value(DeliveryField.SmsKey))
    }

    @Test
    fun `включённый канал сохраняется без получателя — чек уходит на контакт из чека`() {
        val model = model(MemoryCoreSettings(settings()))

        model.switch(DeliveryChannel.Telegram, true)

        assertTrue(model.state.value.savable)
        assertTrue(DeliveryField.of(DeliveryChannel.Telegram).none { model.state.value.malformed(it) })
    }
}
