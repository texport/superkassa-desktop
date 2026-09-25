package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи каналов доставки чека покупателю.
 *
 * Своим файлом: полей у каналов десяток, и у каждого подпись на трёх языках.
 */
data class DeliverySettingTexts(
    val title: String,
    val hint: String,
    val configured: String,
    val notConfigured: String,
    val secretHint: String,
    val malformed: String,
    val portRange: String,
    val recipient: String,
    val saved: String,
    val channels: DeliveryChannelTexts,
    val fields: DeliveryFieldTexts
)

/**
 * Названия каналов доставки.
 *
 * Названия служб одни на всех языках — SMS, Telegram, WhatsApp; своё слово
 * на каждом языке только у почты.
 */
data class DeliveryChannelTexts(
    val sms: String,
    val telegram: String,
    val whatsApp: String,
    val email: String
)

/** Подписи полей настройки каналов доставки. */
data class DeliveryFieldTexts(
    val smsUrl: String,
    val smsKey: String,
    val telegramToken: String,
    val whatsAppToken: String,
    val whatsAppSender: String,
    val emailHost: String,
    val emailPort: String,
    val emailUser: String,
    val emailPassword: String,
    val emailFrom: String
)
