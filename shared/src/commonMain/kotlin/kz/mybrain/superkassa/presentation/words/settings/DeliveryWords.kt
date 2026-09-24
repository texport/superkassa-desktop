package kz.mybrain.superkassa.presentation.words.settings

import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.strings.api.settings.DeliveryChannelNames
import kz.mybrain.superkassa.strings.api.settings.DeliveryFieldNames

/** Название канала доставки чека. */
fun DeliveryChannelNames.of(channel: DeliveryChannel): String = when (channel) {
    DeliveryChannel.Sms -> sms
    DeliveryChannel.Telegram -> telegram
    DeliveryChannel.WhatsApp -> whatsApp
    DeliveryChannel.Email -> email
}

/** Подпись поля настройки канала доставки. */
fun DeliveryFieldNames.of(field: DeliveryField): String = when (field) {
    DeliveryField.SmsUrl -> smsUrl
    DeliveryField.SmsKey -> smsKey
    DeliveryField.TelegramToken -> telegramToken
    DeliveryField.WhatsAppToken -> whatsAppToken
    DeliveryField.WhatsAppSender -> whatsAppSender
    DeliveryField.EmailHost -> emailHost
    DeliveryField.EmailPort -> emailPort
    DeliveryField.EmailUser -> emailUser
    DeliveryField.EmailPassword -> emailPassword
    DeliveryField.EmailFrom -> emailFrom
}
