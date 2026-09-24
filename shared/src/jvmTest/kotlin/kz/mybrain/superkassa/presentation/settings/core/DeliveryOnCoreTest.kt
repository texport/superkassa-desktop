package kz.mybrain.superkassa.presentation.settings.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.domain.settings.model.DeliveryRules
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.settings.SettingsBench
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Каналы доставки чека на настоящем ядре: владелец набирает поля карточки,
 * итог читается из настроек кассы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryOnCoreTest {
    private lateinit var desk: SettingsBench

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        desk = SettingsBench().enter()
    }

    @AfterTest
    fun close() {
        desk.close()
        Dispatchers.resetMain()
    }

    private fun stored() = runBlocking { desk.coreSettings.read() }.delivery

    @Test
    fun `адрес и ключ SMS уходят в кассу, а на экране ключ скрыт`() {
        val model = deliveryModel(desk.app)

        model.type(DeliveryField.SmsUrl, "https://sms.example.kz/send")
        model.type(DeliveryField.SmsKey, SECRET)
        model.save()

        assertEquals("https://sms.example.kz/send", stored()?.sms?.providerUrl)
        assertEquals(SECRET, stored()?.sms?.apiKey)
        assertIs<Message.Done>(desk.notices.last)
        assertEquals(DeliveryRules.HIDDEN, model.state.value.value(DeliveryField.SmsKey))
        assertTrue(DeliveryChannel.Sms in model.state.value.configured)
        assertFalse(model.state.value.toString().contains(SECRET), "ключ попал в состояние экрана")
    }

    @Test
    fun `ключ под знаком остаётся прежним, стёртый — снимается`() {
        val first = deliveryModel(desk.app)
        first.type(DeliveryField.TelegramToken, SECRET)
        first.save()
        val again = deliveryModel(desk.app)

        again.type(DeliveryField.SmsUrl, "https://sms.example.kz")
        again.save()
        assertEquals(SECRET, stored()?.telegram?.botToken, "ключ, которого не касались, потерян")

        again.type(DeliveryField.TelegramToken, "**")
        again.save()
        assertNull(stored()?.telegram?.botToken, "стёртый ключ остался")
        assertFalse(DeliveryChannel.Telegram in again.state.value.configured)
    }

    @Test
    fun `набор поверх знака — новый ключ без звёздочек`() {
        val model = deliveryModel(desk.app)
        model.type(DeliveryField.EmailHost, "smtp.example.kz")
        model.type(DeliveryField.EmailPassword, SECRET)
        model.save()

        model.type(DeliveryField.EmailPassword, DeliveryRules.HIDDEN + "n")
        model.type(DeliveryField.EmailPassword, "new-password")
        model.save()

        assertEquals("new-password", stored()?.email?.password)
    }

    @Test
    fun `негодный порт и адрес названы до сохранения, и сохранить нельзя`() {
        val model = deliveryModel(desk.app)

        model.type(DeliveryField.EmailHost, "smtp.example.kz")
        model.type(DeliveryField.EmailPort, "99999")
        model.type(DeliveryField.SmsUrl, "sms.example.kz")

        assertTrue(model.state.value.malformed(DeliveryField.EmailPort))
        assertTrue(model.state.value.malformed(DeliveryField.SmsUrl))
        assertFalse(model.state.value.savable)
        model.save()
        assertNull(stored()?.email, "негодное сохранено")
    }

    @Test
    fun `доставка и сроки БФД не затирают друг друга`() {
        val core = coreSettingsModel(desk.app)
        val delivery = deliveryModel(desk.app)

        delivery.type(DeliveryField.TelegramToken, SECRET)
        delivery.save()
        core.typeTimeout("9")
        core.save()

        val settings = runBlocking { desk.coreSettings.read() }
        assertEquals(9L, settings.ofdTimeoutSeconds)
        assertEquals(SECRET, settings.delivery?.telegram?.botToken, "сохранение сроков вернуло прежнюю доставку")
    }

    @Test
    fun `ключи каналов не попадают в журнал`() {
        val model = deliveryModel(desk.app)
        model.type(DeliveryField.WhatsAppToken, SECRET)
        model.type(DeliveryField.WhatsAppSender, "105000000000001")
        model.save()

        assertFalse(desk.journal.lines.any { it.contains(SECRET) }, desk.journal.lines.joinToString("\n"))
    }

    private companion object {
        const val SECRET = "bot-key-7f3a91"
    }
}
