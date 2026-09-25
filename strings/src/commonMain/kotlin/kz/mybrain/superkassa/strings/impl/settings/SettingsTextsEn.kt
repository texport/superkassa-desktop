package kz.mybrain.superkassa.strings.impl.settings

import kz.mybrain.superkassa.strings.api.settings.CoreSettingTexts
import kz.mybrain.superkassa.strings.api.settings.DeliveryFieldTexts
import kz.mybrain.superkassa.strings.api.settings.DeliverySettingTexts
import kz.mybrain.superkassa.strings.api.settings.KassaFactsTexts
import kz.mybrain.superkassa.strings.api.settings.LookTexts
import kz.mybrain.superkassa.strings.api.settings.SettingsTexts

/** Надписи [SettingsTexts] по-английски. */
internal val settingsTextsEn = SettingsTexts(
    core = CoreSettingTexts(
        title = "The register on this machine",
        hint = "Shared by every register of the workplace: how long to wait for the BFD and when to try the link " +
            "again. New values take effect after the register restarts.",
        unread = "The register did not return its settings",
        mode = "Operating mode",
        modeDesktop = "Workplace",
        modeServer = "Server",
        reconnect = "BFD reconnect interval, s",
        seconds = "A whole number of seconds above zero",
        protocolFixed = "The protocol version is set when the register starts and is not changed here.",
        frozen = "Editing is locked",
        frozenHint = "The workplace owner locked the register settings in its settings file: here they " +
            "can only be viewed.",
        serverHint = "The register runs as a server: its settings are changed on the server, not at the workplace.",
        saved = "Register settings saved. They take effect after the register restarts",
        autoClose = "Close the shift by itself after a day",
        autoCloseHint = "A shift longer than a day is not allowed: the register stops issuing receipts. " +
            "With this switch the register closes the shift and takes the Z report itself if the cashier did not."
    ),
    delivery = DeliverySettingTexts(
        title = "Receipt delivery to the customer",
        hint = "An enabled channel sends the receipt to the customer through a service: an SMS gateway, " +
            "a Telegram bot, WhatsApp or a mail server. Shared by every register of the workplace. " +
            "Addresses and keys work from the next receipt; enabling a channel — after the register restarts.",
        recipient = "The receipt goes to the customer contact given in the receipt: a phone — by SMS and WhatsApp, " +
            "an email — by mail, a chat — in Telegram. Without a contact the receipt is not sent to the customer.",
        configured = "Set up",
        notConfigured = "Not set up",
        secretHint = "Keys that are set are hidden as ***: clear a key to remove it, or type a new one.",
        malformed = "Check the spelling",
        portRange = "A whole number from 1 to 65535",
        saved = "Receipt delivery saved. Enabled channels take effect after the register restarts",
        channels = deliveryChannels("Email"),
        fields = DeliveryFieldTexts(
            smsUrl = "Gateway address with {phone} and {text}",
            smsKey = "SMS gateway key",
            telegramToken = "Bot token",
            whatsAppToken = "Access key",
            whatsAppSender = "Sender number (ID)",
            emailHost = "Mail server",
            emailPort = "Port",
            emailUser = "User",
            emailPassword = "Password",
            emailFrom = "Sender address"
        )
    ),
    facts = KassaFactsTexts(
        title = "Register facts",
        hint = "What support asks first: which versions are installed, how the register runs and where its data " +
            "lives. Visible before sign-in too — when the register did not open or does not let anyone in.",
        appVersion = "App version",
        coreVersion = "Core version",
        dataDirectory = "Data folder",
        kkmCount = "Registers on this machine",
        unread = "The register did not answer",
        ofdAuth = "BFD authorisation data",
        nextRequest = "Next request number"
    )
)

/** Надписи [LookTexts] по-английски. */
internal val lookTextsEn = LookTexts(
    accent = "Accent",
    accentHint = "Main colour of buttons, selection and icons. Refusal stays red with any accent.",
    accentRed = "Red",
    accentOrange = "Orange",
    accentAmber = "Amber",
    accentOlive = "Olive",
    accentLime = "Lime",
    accentGreen = "Green",
    accentEmerald = "Emerald",
    accentTeal = "Teal",
    accentAzure = "Azure",
    accentBlue = "Blue",
    accentIndigo = "Indigo",
    accentViolet = "Violet",
    accentLilac = "Lilac",
    accentPink = "Pink",
    typeface = "Font",
    typefaceSystem = "System",
    typefaceSans = "Sans-serif",
    typefaceSerif = "Serif",
    typefaceMono = "Monospace",
    textScale = "Size",
    textScaleHint = "Dense for the goods list, the large steps for the till: the total reads from a metre away.",
    textScaleDense = "Dense",
    textScaleCompact = "Compact",
    textScaleNormal = "Regular",
    textScaleLarge = "Large",
    textScaleLarger = "Larger"
)
