package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.EmailProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.WhatsAppProviderSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Правила полей доставки без экрана: знак ключа, годность и «настроен» так, как считает касса. */
class DeliveryRulesTest {

    @Test
    fun `набор поверх знака ключа начинает новый, стирание снимает ключ целиком`() {
        assertEquals("a", DeliveryRules.typedSecret(DeliveryRules.HIDDEN, DeliveryRules.HIDDEN + "a"))
        assertEquals("", DeliveryRules.typedSecret(DeliveryRules.HIDDEN, "**"))
        assertEquals("new", DeliveryRules.typedSecret(DeliveryRules.HIDDEN, "new"))
        assertEquals("ab", DeliveryRules.typedSecret("a", "ab"))
    }

    @Test
    fun `порт — от 1 до 65535, адрес шлюза — со схемой, отправитель — с собакой`() {
        assertTrue(DeliveryRules.valid(DeliveryField.EmailPort, "587"))
        assertFalse(DeliveryRules.valid(DeliveryField.EmailPort, "0"))
        assertFalse(DeliveryRules.valid(DeliveryField.EmailPort, "65536"))
        assertFalse(DeliveryRules.valid(DeliveryField.SmsUrl, "sms.example.kz"))
        assertTrue(DeliveryRules.valid(DeliveryField.SmsUrl, "https://sms.example.kz"))
        assertFalse(DeliveryRules.valid(DeliveryField.EmailFrom, "kassa example.kz"))
        assertTrue(DeliveryRules.valid(DeliveryField.EmailFrom, ""))
    }

    @Test
    fun `пустой сервер снимает почту целиком`() {
        val now = DeliverySettings(email = EmailProviderSettings(host = "smtp.example.kz"))

        assertNull(DeliveryRules.applied(now, mapOf(DeliveryField.EmailHost to " ")).email)
    }

    @Test
    fun `WhatsApp без номера отправителя касса настроенным не считает`() {
        val half = DeliverySettings(whatsapp = WhatsAppProviderSettings(accessToken = "key"))

        assertFalse(DeliveryChannel.WhatsApp in DeliveryRules.configured(half))
        assertEquals(emptySet(), DeliveryRules.configured(null))
    }

    @Test
    fun `включение ложится в маршрут канала, а получателя у канала нет`() {
        val saved = DeliveryRules.applied(null, emptyMap(), mapOf(DeliveryChannel.Sms to true))

        val route = saved.channels.single()
        assertEquals(listOf<Any?>("SMS", true, null), listOf(route.channel, route.enabled, route.destination))
        assertEquals(setOf(DeliveryChannel.Sms), DeliveryRules.enabled(saved))
        assertEquals(listOf(DeliveryField.SmsUrl, DeliveryField.SmsKey), DeliveryField.of(DeliveryChannel.Sms))
    }
}
