package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.DeliveryTexts

/** Надписи [DeliveryTexts] по-казахски. */
internal val deliveryTextsKk = DeliveryTexts(
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
