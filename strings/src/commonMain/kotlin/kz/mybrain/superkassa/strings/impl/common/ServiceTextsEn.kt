package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.QueueTexts
import kz.mybrain.superkassa.strings.api.common.SettingsScreenTexts
import kz.mybrain.superkassa.strings.api.common.UserTexts
import kz.mybrain.superkassa.strings.impl.settings.lookTextsEn

/** Надписи [QueueTexts] по-английски. */
internal val queueTextsEn = QueueTexts(
    title = "Delivery queue",
    empty = "Nothing is waiting — everything reached the BFD.",
    waiting = "Waiting to be sent",
    attempts = "Attempts",
    retryFailed = "Retry failed",
    retryDone = "Failed tasks queued for another attempt"
)

/** Надписи [UserTexts] по-английски. */
internal val userTextsEn = UserTexts(
    title = "Cashiers and PINs",
    name = "Name",
    create = "Add",
    created = "added",
    newPin = "New PIN",
    change = "Change",
    changed = "PIN changed",
    delete = "Delete",
    deleted = "deleted",
    role = "Role",
    admin = "Administrator",
    cashier = "Cashier"
)

/** Надписи [SettingsScreenTexts] по-английски. */
internal val settingsScreenTextsEn = SettingsScreenTexts(
    title = "Settings",
    currentKkm = "Register in use",
    orgUnknown = "No organization on record",
    changeKkm = "Change register",
    registerKkm = "Add a register",
    back = "Back",
    kkmIdentifier = "Register identifier",
    token = "Token",
    adminPin = "Administrator PIN",
    registering = "Adding…",
    register = "Add",
    registered = "Register added, state",
    diagnostics = "Diagnostics",
    ofdLink = "BFD link",
    checkOfdLink = "Check the BFD link",
    ofdAnswers = "BFD answers",
    ofdSilent = "BFD is silent",
    ofdInfo = "BFD information",
    programmingMode = "Programming mode",
    programmingOn = "On",
    enteredProgramming = "The register entered programming mode",
    exitedProgramming = "The register left programming mode",
    ofd = "BFD",
    environment = "Environment",
    language = "Language",
    appearance = "Appearance",
    appearanceHint = "The look of this workplace. Kept on this machine: receipts, other cash registers " +
        "and the cabinet stay as they are",
    look = lookTextsEn,
    ofdToken = "BFD token",
    newToken = "New token",
    saveToken = "Save token",
    tokenSaved = "Token saved",
    tokenHint = "The BFD issues the token. On an \"invalid token\" answer the register stops " +
        "and works again only after a new one is entered.",
    printForm = "Printed receipt",
    printFormHint = "How the printed receipt looks: language, paper width and what exactly to print. The form must " +
        "match what went to the BFD — it is the same document",
    programmingRequired = "Register settings change in programming mode.",
    printFormSaved = "Receipt settings saved",
    receiptLanguage = "Receipt language",
    receiptBoth = "Both languages",
    receiptKk = "Қазақша",
    receiptRu = "Русский",
    paperWidth = "Paper width",
    printLayout = "Print layout",
    printer = "Register printer",
    printerHint = "The printer belongs to the register: one computer may hold two, " +
        "each with its own receipt tape. Until one is chosen, jobs go to the system default.",
    printerSystem = "System default",
    printerNone = "This machine has no printer at all: there is nowhere to print a receipt. " +
        "Connect a printer and open the settings again",
    printKind = "File kind when saving",
    cabinetServer = "Cabinet server IP",
    cabinetServerHint = "If the cabinet name is not found on the network (a VPN without its own DNS), enter the " +
        "server IP: the cabinet opens through it under its own name. Empty means the network " +
        "finds the name",
    printKindHint = "On screen the receipt is always shown as a picture — the file kind does not affect the " +
        "preview. It sets the file the receipt is saved as: PDF for sending and printing, PNG as " +
        "a picture, HTML as a browser page. On Android printing goes through the system dialog as" +
        " PDF",
    printCopies = "Copies when printing",
    panelBehaviour = "Collapsed when the sale opens",
    taxSettings = "Register taxes",
    taxSettingsHint = "The tax regime and VAT rate of this cash register: the tax of every receipt is computed from " +
        "them. Set them from the KGD records — a mismatch sends receipts out with the wrong tax",
    taxRegime = "Tax regime",
    tradeDomain = "Register industry",
    tradeDomainHint = "The protocol requires an industry on every receipt, and a register works in one: the choice " +
        "made here decides which details the cashier fills in on the sale screen. Trading has none",
    domainKind = "Industry",
    domainFields = "The cashier fills in on the sale screen",
    defaultVatGroup = "Default VAT rate",
    dictionariesMissing = "Register dictionaries were not read",
    dictionariesMissingHint = "Tax regimes and VAT rates come from the register. The register did not answer when " +
        "the workplace asked for them — try again.",
    autoCashout = "Cash out when the shift closes",
    autoCashoutHint = "The register issues the cash withdrawal together with the Z report. Without it the money in " +
        "the drawer carries over into the next shift and the register total stops matching the drawer.",
    settingsSaved = "Register settings saved",
    addressMalformed = "The address starts with http:// or https:// and contains no spaces",
    mapServices = "Map services",
    mapServicesHint = "Until an address is set, the community map is used: it is not meant for every owner",
    mapProvider = "Map",
    mapProviderHint = "No map needs a key. Addresses are searched on the open community map whichever map is chosen. " +
        "Your own tile server below overrides the chosen map",
    mapProviderOsm = "Open data, no display restrictions. Default",
    mapProvider2gis = "Detailed city plans of Kazakhstan",
    mapProviderLocal = "Labels in the cashier’s language",
    mapTiles = "Map tiles",
    mapSearch = "Address search",
    mapReverse = "Address by marker",
    mapLocation = "Location lookup",
    mapDefault = "Back to community",
    panelBehaviourHint = "On — the section is collapsed to its title when the sale opens, off — expanded. " +
        "On the sale screen a section is collapsed and expanded with the arrow in its title — " +
        "the choice is remembered and shown here. Manual item entry is always open for a new receipt.",
    panelPositionEntry = "Manual item entry",
    panelPositionEntryHint = "Name, price and quantity under the barcode field. The barcode is always visible",
    panelReceiptChanges = "Discounts and markups",
    panelReceiptChangesHint = "Discount and markup fields for the whole receipt",
    panelCustomerData = "Customer details",
    panelCustomerDataHint = "Contact for the receipt and buyer details",
    panelMoney = "Payment",
    panelMoneyHint = "Payment types, cash taken and change",
    panelTill = "The register on phone and tablet",
    panelTillHint = "The bottom sheet with payment and discounts. On a wide screen the register is always visible",
    printLayoutHint = "58 and 80 mm tape are for receipt printers; the page is for plain paper " +
        "and for sending to the customer.",
    layoutTape58 = "58 mm tape",
    layoutTape80 = "80 mm tape",
    layoutFullscreen = "Full page",
    printOfdAds = "Print BFD advertising",
    printOfdAdsHint = "The lines arrive with the receipt response and print under the total.",
    receiptLines = "Your own receipt lines",
    receiptLinesHint = "The operator's advertising comes from the BFD; these are the shop's own lines: " +
        "a greeting, return terms, a thank-you. An empty field is not printed.",
    saveReceiptLines = "Save lines",
    lineBeforeHeader = "Above the header",
    lineHeader = "In the header",
    lineAfterHeader = "Below the header",
    lineBeforeItems = "Before the items",
    lineAfterItems = "After the items",
    lineBeforeTotals = "Before the total",
    lineAfterTotals = "After the total",
    lineBeforeQr = "Before the QR code",
    lineFooter = "In the footer",
    appearanceSystem = "Follow system",
    appearanceLight = "Light",
    appearanceDark = "Dark",
    factoryStep = "Step 1. Factory details",
    generateFactory = "Generate",
    factoryNumber = "Serial number",
    manufactureYear = "Year of manufacture",
    ofdStep = "Step 2. Details from the BFD",
    localName = "Register name",
    localNameHint = "The name is kept in the register and shows on the sign-in screen. " +
        "Other register details come from the BFD and are not edited here.",
    save = "Save"
)
