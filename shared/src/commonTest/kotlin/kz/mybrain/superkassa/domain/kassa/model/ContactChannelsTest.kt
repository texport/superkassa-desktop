package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.SmsProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.WhatsAppProviderSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Какой вид контакта настроен — тем же правилом, что у каналов ядра. */
class ContactChannelsTest {

    private val sms = SmsProviderSettings(providerUrl = "https://sms.example.kz/send?to={phone}")

    @Test
    fun `канал без провайдера не настроен, как и провайдер без включённого канала`() {
        val routeOnly = DeliverySettings(channels = listOf(DeliveryChannelSettings("SMS")))
        val providerOnly = DeliverySettings(sms = sms)
        val off = listOf(DeliveryChannelSettings("SMS", enabled = false))
        val switchedOff = DeliverySettings(channels = off, sms = sms)

        assertTrue(ContactChannels.of(routeOnly).none)
        assertTrue(ContactChannels.of(providerOnly).none)
        assertTrue(ContactChannels.of(switchedOff).none)
        assertTrue(ContactChannels.of(null).none)
    }

    @Test
    fun `телефон — это SMS или WhatsApp`() {
        val whatsApp = DeliverySettings(
            channels = listOf(DeliveryChannelSettings("whatsapp")),
            whatsapp = WhatsAppProviderSettings(accessToken = "key", phoneNumberId = "77010000000")
        )
        val halfWhatsApp = whatsApp.copy(whatsapp = WhatsAppProviderSettings(accessToken = "key"))

        assertEquals(setOf(ContactKind.Phone), ContactChannels.of(whatsApp).ready)
        assertTrue(ContactChannels.of(halfWhatsApp).none, "WhatsApp без номера отправителя сочтён настроенным")
    }

    @Test
    fun `«не отправлять» доступно всегда, прочее — по каналу`() {
        val phoneOnly = ContactChannels(setOf(ContactKind.Phone))
        val typed = BuyerContact(ContactKind.Phone, "7017654321")

        assertTrue(phoneOnly.allows(ContactKind.None))
        assertEquals(typed, phoneOnly.choose(typed, ContactKind.Email))
        assertEquals(ContactKind.None, phoneOnly.choose(typed, ContactKind.None).kind)
        assertEquals(BuyerContact(), ContactChannels().fit(typed))
    }

    @Test
    fun `«не отправлять» — контакта в чеке нет, даже с набранным`() {
        val silent = BuyerContact(ContactKind.Phone, "7017654321").switchTo(ContactKind.None)

        assertTrue(silent.empty)
        assertEquals(null, silent.toRequest())
        assertEquals(ContactKind.None, BuyerContact().kind)
    }
}
