package kz.mybrain.superkassa.presentation.strings.journal

import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи окна «Доставка чека покупателю» в журнале документов.
 *
 * Отдельно от надписей журнала: окно своё, и поля его не должны
 * теряться среди столбцов и отбора.
 *
 * Названия каналов — свои, а не коды ядра: `WHATSAPP` и `EMAIL` на экране
 * кассира не пишутся. Незнакомый канал называется словами [otherChannel],
 * а не кодом.
 */
data class DeliveryTexts(
    val title: String,
    val resend: String,
    val close: String,
    val pending: String,
    val delivered: String,
    val failed: String,
    val attempts: String,
    val nextAttempt: String,

    /** Касса не ответила, что с доставкой: это не «доставки нет». */
    val unread: String,
    val notOrdered: String,
    val notOrderedHint: String,

    /** Чек пробит без связи: задачи доставки ставятся, когда БФД его примет. */
    val awaitingBfd: String,
    val awaitingBfdHint: String,
    val sms: String,
    val telegram: String,
    val whatsapp: String,
    val email: String,
    val printer: String,
    val otherChannel: String,
    val link: String,
    val image: String,
    val page: String
)

private val deliveryTextsRu = DeliveryTexts(
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

private val deliveryTextsKk = DeliveryTexts(
    title = "Чекті жеткізу",
    resend = "Қайта жіберу",
    close = "Жабу",
    pending = "Жіберуді күтуде",
    delivered = "Жеткізілді",
    failed = "Сәтсіз",
    attempts = "Әрекет саны",
    nextAttempt = "Келесі әрекет",
    unread = "Касса чекті жеткізу туралы жауап бермеді. Чекті қайта ашыңыз.",
    notOrdered = "Чек сатып алушыға жіберілмеген",
    notOrderedHint = "SMS, мессенджер немесе пошта арқылы жеткізуді касса иесі баптайды. " +
        "Қағаз чекті журналдан басып шығаруға болады.",
    awaitingBfd = "Чекті БФД әлі қабылдамады",
    awaitingBfdHint = "Касса оны БФД-ға жеткізген бойда сатып алушыға өзі кетеді.",
    sms = "SMS",
    telegram = "Telegram",
    whatsapp = "WhatsApp",
    email = "Пошта",
    printer = "Чек принтері",
    otherChannel = "Басқа арна",
    link = "сілтеме",
    image = "сурет",
    page = "веб-бет"
)

private val deliveryTextsEn = DeliveryTexts(
    title = "Receipt delivery",
    resend = "Send again",
    close = "Close",
    pending = "Waiting to send",
    delivered = "Delivered",
    failed = "Failed",
    attempts = "Attempts",
    nextAttempt = "Next attempt",
    unread = "The cash register did not say how the receipt delivery went. Open the receipt again.",
    notOrdered = "The receipt was not sent to the customer",
    notOrderedHint = "Delivery by SMS, messenger or e-mail is set up by the cash register owner. " +
        "A paper receipt can be printed from the journal.",
    awaitingBfd = "The BFD has not accepted the receipt yet",
    awaitingBfdHint = "It will go to the customer by itself once the cash register delivers it to the BFD.",
    sms = "SMS",
    telegram = "Telegram",
    whatsapp = "WhatsApp",
    email = "E-mail",
    printer = "Receipt printer",
    otherChannel = "Other channel",
    link = "link",
    image = "image",
    page = "web page"
)

/** Надписи окна доставки на выбранном языке. */
fun deliveryTexts(language: Language): DeliveryTexts = when (language) {
    Language.Kk -> deliveryTextsKk
    Language.Ru -> deliveryTextsRu
    Language.En -> deliveryTextsEn
}
