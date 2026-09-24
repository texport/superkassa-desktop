package kz.mybrain.superkassa.presentation.strings.settings

import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи каналов доставки чека покупателю.
 *
 * Своим файлом: полей у каналов десяток, и у каждого подпись на трёх языках.
 */
data class DeliveryTexts(
    val title: String,
    val hint: String,
    val configured: String,
    val notConfigured: String,
    val secretHint: String,
    val malformed: String,
    val portRange: String,
    val recipient: String,
    val saved: String,
    val channels: Map<DeliveryChannel, String>,
    val fields: Map<DeliveryField, String>
)

/** Надписи каналов доставки на выбранном языке. */
fun deliveryTexts(language: Language): DeliveryTexts = when (language) {
    Language.Kk -> kazakhDelivery
    Language.Ru -> russianDelivery
    Language.En -> englishDelivery
}

private val russianDelivery = DeliveryTexts(
    title = "Доставка чека покупателю",
    hint = "Включённый канал отправляет чек покупателю через службу: SMS-шлюз, бота Telegram, WhatsApp " +
        "или почтовый сервер. Одни на все кассы рабочего места. Адреса и ключи действуют со следующего чека, " +
        "включение канала — после перезапуска кассы.",
    recipient = "Чек уходит на контакт покупателя, указанный в чеке: телефон — по SMS и в WhatsApp, " +
        "почта — письмом, чат — в Telegram. Без контакта чек покупателю не отправляется.",
    configured = "Настроен",
    notConfigured = "Не настроен",
    secretHint = "Заданные ключи скрыты знаком ***: сотрите ключ, чтобы убрать его, или наберите новый.",
    malformed = "Проверьте написание",
    portRange = "Целое число от 1 до 65535",
    saved = "Доставка чека сохранена. Включение канала подействует после перезапуска кассы",
    channels = channelNames("Почта"),
    fields = mapOf(
        DeliveryField.SmsUrl to "Адрес шлюза с {phone} и {text}",
        DeliveryField.SmsKey to "Ключ SMS-шлюза",
        DeliveryField.TelegramToken to "Токен бота",
        DeliveryField.WhatsAppToken to "Ключ доступа",
        DeliveryField.WhatsAppSender to "Номер отправителя (ID)",
        DeliveryField.EmailHost to "Почтовый сервер",
        DeliveryField.EmailPort to "Порт",
        DeliveryField.EmailUser to "Пользователь",
        DeliveryField.EmailPassword to "Пароль",
        DeliveryField.EmailFrom to "Адрес отправителя"
    )
)

private val kazakhDelivery = DeliveryTexts(
    title = "Чекті сатып алушыға жеткізу",
    hint = "Қосылған арна чекті сатып алушыға қызмет арқылы жібереді: SMS шлюзі, Telegram боты, WhatsApp " +
        "немесе пошта сервері. Жұмыс орнының барлық кассасына ортақ. Мекенжайлар мен кілттер келесі чектен, " +
        "арнаны қосу касса қайта іске қосылғаннан кейін әрекет етеді.",
    recipient = "Чек сатып алушының чекте көрсетілген байланысына жіберіледі: телефон — SMS және WhatsApp арқылы, " +
        "пошта — хатпен, чат — Telegram-ға. Байланыс болмаса, чек сатып алушыға жіберілмейді.",
    configured = "Бапталған",
    notConfigured = "Бапталмаған",
    secretHint = "Берілген кілттер *** белгісімен жасырылған: кілтті алып тастау үшін өшіріңіз " +
        "немесе жаңасын теріңіз.",
    malformed = "Жазылуын тексеріңіз",
    portRange = "1-ден 65535-ке дейінгі бүтін сан",
    saved = "Чекті жеткізу сақталды. Арнаны қосу касса қайта іске қосылғаннан кейін әрекет етеді",
    channels = channelNames("Пошта"),
    fields = mapOf(
        DeliveryField.SmsUrl to "{phone} және {text} бар шлюз мекенжайы",
        DeliveryField.SmsKey to "SMS шлюзінің кілті",
        DeliveryField.TelegramToken to "Бот токені",
        DeliveryField.WhatsAppToken to "Қол жеткізу кілті",
        DeliveryField.WhatsAppSender to "Жіберуші нөмірі (ID)",
        DeliveryField.EmailHost to "Пошта сервері",
        DeliveryField.EmailPort to "Порт",
        DeliveryField.EmailUser to "Пайдаланушы",
        DeliveryField.EmailPassword to "Құпиясөз",
        DeliveryField.EmailFrom to "Жіберуші мекенжайы"
    )
)

private val englishDelivery = DeliveryTexts(
    title = "Receipt delivery to the customer",
    hint = "An enabled channel sends the receipt to the customer through a service: an SMS gateway, " +
        "a Telegram bot, WhatsApp or a mail server. Shared by every register of the workplace. Addresses and keys " +
        "work from the next receipt; enabling a channel — after the register restarts.",
    recipient = "The receipt goes to the customer contact given in the receipt: a phone — by SMS and WhatsApp, " +
        "an email — by mail, a chat — in Telegram. Without a contact the receipt is not sent to the customer.",
    configured = "Set up",
    notConfigured = "Not set up",
    secretHint = "Keys that are set are hidden as ***: clear a key to remove it, or type a new one.",
    malformed = "Check the spelling",
    portRange = "A whole number from 1 to 65535",
    saved = "Receipt delivery saved. Enabled channels take effect after the register restarts",
    channels = channelNames("Email"),
    fields = mapOf(
        DeliveryField.SmsUrl to "Gateway address with {phone} and {text}",
        DeliveryField.SmsKey to "SMS gateway key",
        DeliveryField.TelegramToken to "Bot token",
        DeliveryField.WhatsAppToken to "Access key",
        DeliveryField.WhatsAppSender to "Sender number (ID)",
        DeliveryField.EmailHost to "Mail server",
        DeliveryField.EmailPort to "Port",
        DeliveryField.EmailUser to "User",
        DeliveryField.EmailPassword to "Password",
        DeliveryField.EmailFrom to "Sender address"
    )
)

/** Названия служб одни на всех языках, кроме почты. */
private fun channelNames(email: String) = mapOf(
    DeliveryChannel.Sms to "SMS",
    DeliveryChannel.Telegram to "Telegram",
    DeliveryChannel.WhatsApp to "WhatsApp",
    DeliveryChannel.Email to email
)
