package kz.mybrain.superkassa.strings.impl.kassa.contact

import kz.mybrain.superkassa.strings.api.kassa.contact.BuyerContactTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactFieldTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactKindTexts

/** Надписи [BuyerContactTexts] по-русски. */
internal val buyerContactTextsRu = BuyerContactTexts(
    kinds = ContactKindTexts(
        none = "Не отправлять",
        phone = "Телефон",
        email = "Почта",
        telegram = "Telegram"
    ),
    labels = ContactFieldTexts(
        phone = "Телефон покупателя",
        email = "Почта покупателя",
        telegram = "Чат покупателя в Telegram (ID)"
    ),
    formats = ContactFieldTexts(
        phone = "Номер Казахстана: +7 7XX XXX XX XX",
        email = "Почта вида name@example.kz",
        telegram = "Номер чата цифрами: бот пишет только по нему"
    ),
    hint = "Необязательно: чек уйдёт покупателю на этот контакт",
    sendsTo = "Чек уйдёт на",
    notConfigured = "Не настроен",
    unavailable = "Доставка чека не настроена — чек можно показать покупателю или распечатать"
)
