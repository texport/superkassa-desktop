package kz.mybrain.superkassa.strings.impl.share

import kz.mybrain.superkassa.strings.api.common.ShareTexts

/** Надписи [ShareTexts] по-английски. */
internal val shareTextsEn = ShareTexts(
    share = "Share",
    whatsApp = "WhatsApp",
    telegram = "Telegram",
    email = "Email",
    subject = "Electronic receipt",
    withLink = "Electronic receipt: %s",
    withoutLink = "Electronic receipt — attached",
    noLink = "There is no receipt link yet: the BFD has not accepted it. " +
        "Save the file and send it to the buyer yourself",
    failed = "Could not open an app to send it. Save the file and send it to the buyer yourself"
)
