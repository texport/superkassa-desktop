package kz.mybrain.superkassa.strings.impl.settings

import kz.mybrain.superkassa.strings.api.settings.DeliveryChannelNames

/** Названия каналов доставки: службы названы одинаково на всех языках, кроме почты. */
internal fun deliveryChannels(email: String) = DeliveryChannelNames(
    sms = "SMS",
    telegram = "Telegram",
    whatsApp = "WhatsApp",
    email = email
)
