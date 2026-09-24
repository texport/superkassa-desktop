package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.DeliveryTexts

/** Надписи [DeliveryTexts] по-русски. */
internal val deliveryTextsRu = DeliveryTexts(
    title = "Доставка чека",
    resend = "Отправить ещё раз",
    close = "Закрыть",
    pending = "Ждёт отправки",
    delivered = "Доставлен",
    failed = "Не удалось",
    attempts = "Попыток",
    nextAttempt = "Следующая попытка",
    unread = "Касса не ответила, что с доставкой чека. Откройте чек ещё раз.",
    notOrdered = "Чек покупателю не отправлялся",
    notOrderedHint = "Доставку по SMS, в мессенджер или на почту настраивает владелец кассы. " +
        "Бумажный чек можно напечатать из журнала.",
    awaitingBfd = "Чек ещё не принят БФД",
    awaitingBfdHint = "Покупателю он уйдёт сам, как только касса дошлёт его в БФД.",
    sms = "SMS",
    telegram = "Telegram",
    whatsapp = "WhatsApp",
    email = "Почта",
    printer = "Принтер чеков",
    otherChannel = "Другой канал",
    link = "ссылка",
    image = "картинка",
    page = "веб-страница"
)
