package kz.mybrain.superkassa.presentation.settings.core

import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.strings.api.settings.DeliveryChannelTexts
import kz.mybrain.superkassa.strings.api.settings.DeliveryFieldTexts

/** Название канала доставки чека. */
internal fun DeliveryChannelTexts.of(channel: DeliveryChannel): String = when (channel) {
    DeliveryChannel.Sms -> sms
    DeliveryChannel.Telegram -> telegram
    DeliveryChannel.WhatsApp -> whatsApp
    DeliveryChannel.Email -> email
}

/** Подпись поля настройки канала доставки. */
internal fun DeliveryFieldTexts.of(field: DeliveryField): String = when (field) {
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
