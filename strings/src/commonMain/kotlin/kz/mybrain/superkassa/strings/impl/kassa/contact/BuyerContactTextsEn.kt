package kz.mybrain.superkassa.strings.impl.kassa.contact

import kz.mybrain.superkassa.strings.api.kassa.contact.BuyerContactTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactFieldTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactKindTexts

/** Надписи [BuyerContactTexts] по-английски. */
internal val buyerContactTextsEn = BuyerContactTexts(
    kinds = ContactKindTexts(
        none = "Don't send",
        phone = "Phone",
        email = "Email",
        telegram = "Telegram"
    ),
    labels = ContactFieldTexts(
        phone = "Customer phone",
        email = "Customer email",
        telegram = "Customer Telegram chat (ID)"
    ),
    formats = ContactFieldTexts(
        phone = "A Kazakhstan number: +7 7XX XXX XX XX",
        email = "An address like name@example.kz",
        telegram = "The chat number in digits: the bot writes only to it"
    ),
    hint = "Optional: the receipt goes to the customer at this contact",
    sendsTo = "The receipt goes to",
    notConfigured = "Not set up",
    unavailable = "Send the receipt to the buyer with Share once it is issued — or show it and print it"
)
