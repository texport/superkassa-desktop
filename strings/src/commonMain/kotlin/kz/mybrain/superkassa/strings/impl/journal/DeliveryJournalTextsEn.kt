package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.DeliveryJournalTexts

/** Надписи [DeliveryJournalTexts] по-английски. */
internal val deliveryJournalTextsEn = DeliveryJournalTexts(
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
