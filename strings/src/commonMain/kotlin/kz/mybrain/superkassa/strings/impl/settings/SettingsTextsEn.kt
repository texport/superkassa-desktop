package kz.mybrain.superkassa.strings.impl.settings

import kz.mybrain.superkassa.strings.api.settings.CoreSettingTexts
import kz.mybrain.superkassa.strings.api.settings.KassaFactsTexts
import kz.mybrain.superkassa.strings.api.settings.LookTexts
import kz.mybrain.superkassa.strings.api.settings.SettingsTexts

/** Надписи [SettingsTexts] по-английски. */
internal val settingsTextsEn = SettingsTexts(
    core = CoreSettingTexts(
        title = "BFD exchange timeouts",
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
    facts = KassaFactsTexts(
        title = "Application and kassa core",
        hint = "What support asks first: which versions are installed, how the register runs and where its data " +
            "lives. Visible before sign-in too — when the register did not open or does not let anyone in.",
        appVersion = "App version",
        coreVersion = "Core version",
        dataDirectory = "Data folder",
        kkmCount = "Registers on this machine",
        unread = "The register did not answer",
        ofdAuth = "BFD authorisation data",
        nextRequest = "Next request number"
    ),
    sections = settingsSectionsEn
)

/** Надписи [LookTexts] по-английски. */
internal val lookTextsEn = LookTexts(
    theme = "Theme",
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
