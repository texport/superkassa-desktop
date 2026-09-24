package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.EmailProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.SmsProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.TelegramProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.WhatsAppProviderSettings

/**
 * Канал, по которому касса сама отправляет чек.
 *
 * Бумажный чек сюда не входит: его печатает само приложение, а не касса.
 *
 * @property code имя канала в настройках кассы.
 */
enum class DeliveryChannel(val code: String) {
    Sms("SMS"),
    Telegram("TELEGRAM"),
    WhatsApp("WHATSAPP"),
    Email("EMAIL")
}

/**
 * Поле настроек канала доставки: чьё оно, как читается из настроек кассы
 * и как в них пишется.
 *
 * Получателя среди полей нет: чек уходит на контакт покупателя, указанный
 * в самом чеке, а не на один номер для всех чеков.
 *
 * Перечисление, а не десяток полей формы подряд: состав задан настройками
 * кассы, и новое поле — строка здесь, а не копия разметки и правила.
 *
 * @property channel канал, которому принадлежит поле.
 * @property secret ключ канала: кто его прочитал, тот шлёт сообщения
 *   от имени кассы. На экран выходит только знак того, что он задан.
 */
enum class DeliveryField(
    val channel: DeliveryChannel,
    val secret: Boolean,
    internal val read: (DeliverySettings) -> String?,
    internal val write: (DeliverySettings, String?) -> DeliverySettings
) {
    SmsUrl(
        DeliveryChannel.Sms,
        false,
        { it.sms?.providerUrl },
        { d, v -> d.copy(sms = d.smsOf().copy(providerUrl = v)) }
    ),
    SmsKey(DeliveryChannel.Sms, true, { it.sms?.apiKey }, { d, v -> d.copy(sms = d.smsOf().copy(apiKey = v)) }),
    TelegramToken(
        DeliveryChannel.Telegram,
        true,
        { it.telegram?.botToken },
        { d, v -> d.copy(telegram = TelegramProviderSettings(botToken = v)) }
    ),
    WhatsAppToken(
        DeliveryChannel.WhatsApp,
        true,
        { it.whatsapp?.accessToken },
        { d, v -> d.copy(whatsapp = d.whatsAppOf().copy(accessToken = v)) }
    ),
    WhatsAppSender(
        DeliveryChannel.WhatsApp,
        false,
        { it.whatsapp?.phoneNumberId },
        { d, v -> d.copy(whatsapp = d.whatsAppOf().copy(phoneNumberId = v)) }
    ),
    EmailHost(DeliveryChannel.Email, false, { it.email?.host }, { d, v -> d.withEmail { copy(host = v.orEmpty()) } }),
    EmailPort(
        DeliveryChannel.Email,
        false,
        { it.email?.port?.toString() },
        { d, v -> d.withEmail { copy(port = v?.toIntOrNull() ?: port) } }
    ),
    EmailUser(DeliveryChannel.Email, false, { it.email?.user }, { d, v -> d.withEmail { copy(user = v) } }),
    EmailPassword(DeliveryChannel.Email, true, { it.email?.password }, { d, v -> d.withEmail { copy(password = v) } }),
    EmailFrom(DeliveryChannel.Email, false, { it.email?.from }, { d, v -> d.withEmail { copy(from = v.orEmpty()) } });

    /** Поля одного канала в порядке формы. */
    companion object {
        fun of(channel: DeliveryChannel): List<DeliveryField> = entries.filter { it.channel == channel }
    }
}

/** Маршрут канала в настройках кассы: включён ли он. */
internal fun DeliverySettings.route(channel: DeliveryChannel): DeliveryChannelSettings? =
    channels.firstOrNull { it.channel.equals(channel.code, ignoreCase = true) }

/**
 * Маршрут канала с правкой [change]; маршрута не было — заводится
 * выключенным.
 */
internal fun DeliverySettings.withRoute(
    channel: DeliveryChannel,
    change: DeliveryChannelSettings.() -> DeliveryChannelSettings
): DeliverySettings {
    val old = route(channel)
    val now = old ?: DeliveryChannelSettings(channel = channel.code, enabled = false)
    return copy(channels = channels.filterNot { it === old } + now.change())
}

private fun DeliverySettings.smsOf() = sms ?: SmsProviderSettings()

private fun DeliverySettings.whatsAppOf() = whatsapp ?: WhatsAppProviderSettings()

/**
 * Почта с правкой [change]. Пустой адрес сервера снимает почту целиком:
 * у почты с пустым сервером касса считала бы канал настроенным и отвечала
 * покупателю сбоем соединения вместо «канал не настроен».
 */
private fun DeliverySettings.withEmail(change: EmailProviderSettings.() -> EmailProviderSettings): DeliverySettings {
    val changed = (email ?: EmailProviderSettings(host = "")).change()
    return copy(email = changed.takeIf { it.host.isNotBlank() })
}
