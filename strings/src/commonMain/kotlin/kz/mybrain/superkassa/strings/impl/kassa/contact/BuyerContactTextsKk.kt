package kz.mybrain.superkassa.strings.impl.kassa.contact

import kz.mybrain.superkassa.strings.api.kassa.contact.BuyerContactTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactFieldTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactKindTexts

/** Надписи [BuyerContactTexts] по-казахски. */
internal val buyerContactTextsKk = BuyerContactTexts(
    kinds = ContactKindTexts(
        none = "Жібермеу",
        phone = "Телефон",
        email = "Пошта",
        telegram = "Telegram"
    ),
    labels = ContactFieldTexts(
        phone = "Сатып алушының телефоны",
        email = "Сатып алушының поштасы",
        telegram = "Сатып алушының Telegram чаты (ID)"
    ),
    formats = ContactFieldTexts(
        phone = "Қазақстан нөмірі: +7 7XX XXX XX XX",
        email = "name@example.kz түріндегі пошта",
        telegram = "Чат нөмірі цифрмен: бот тек соған жазады"
    ),
    hint = "Міндетті емес: чек сатып алушыға осы байланысқа жіберіледі",
    sendsTo = "Чек жіберіледі:",
    notConfigured = "Бапталмаған",
    unavailable = "Чекті жеткізу бапталмаған — чекті сатып алушыға көрсетуге немесе басып шығаруға болады"
)
