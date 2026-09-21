package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи служебных разделов на этом языке.
 *
 * Настройки, показ печатной формы и словари состояний вынесены из общего
 * набора: одних настроек сто строк, и рядом с ними надписи продажи
 * не найти глазами.
 */

internal val englishSettings = SettingStrings(
    title = "Settings",
    currentKkm = "Register in use",
    changeKkm = "Change register",
    registerKkm = "Add a register",
    back = "Back",
    registerKkmHint = "The first register is added before signing in: the node issues a factory number, " +
        "then the register is enrolled in the BFD cabinet and its id and token are entered here.",
    kkmIdentifier = "Register identifier",
    token = "Token",
    adminPin = "Administrator PIN",
    registering = "Adding…",
    register = "Add",
    registered = "Register added, state",
    registerHint = "The administrator PIN is set here and the new register will have no other: " +
        "the node refuses the default one, and it can only be changed from inside. " +
        "Cashiers and their PINs are added later under “Cashiers and PINs”.",
    diagnostics = "Diagnostics",
    ofdLink = "BFD link",
    checkOfdLink = "Check the BFD link",
    ofdAnswers = "BFD answers",
    ofdSilent = "BFD is silent",
    ofdInfo = "BFD information",
    programmingMode = "Programming mode",
    enterProgramming = "Enter programming",
    exitProgramming = "Exit",
    enteredProgramming = "The register entered programming mode",
    exitedProgramming = "The register left programming mode",
    ofd = "BFD",
    environment = "Environment",
    language = "Language",
    appearance = "Appearance",
    appearanceHint = "The theme and language of this workplace. Kept on this machine: receipts, other cash registers and the " +
        "cabinet stay as they are",
    ofdToken = "BFD token",
    newToken = "New token",
    saveToken = "Save token",
    tokenSaved = "Token saved",
    tokenHint = "The BFD issues the token. On an \"invalid token\" answer the register stops " +
        "and works again only after a new one is entered.",
    printForm = "Printed receipt",
    printFormHint = "How the printed receipt looks: language, paper width and what exactly to print. The form must match what " +
        "went to the BFD — it is the same document",
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
    printKind = "File kind when saving",
    printCopies = "Copies when printing",
    panelBehaviour = "Till column sections",
    taxSettings = "Register taxes",
    taxSettingsHint = "The tax regime and VAT rate of this cash register: the tax of every receipt is computed from them. Set them " +
        "from the KGD records — a mismatch sends receipts out with the wrong tax",
    taxRegime = "Tax regime",
    defaultVatGroup = "Default VAT rate",
    autoCloseShift = "Close the shift automatically",
    autoCloseShiftHint = "A shift lasts a day. The node closes it on its own if the cashier did not — the Z report is still taken on " +
        "time.",
    settingsSaved = "Register settings saved",
    node = "Node",
    nodeHint = "The node is the service on this machine that talks to the BFD: the cash register hands it a command, it " +
        "signs and sends. Here you see whether it answers, which build it is and where its database lives",
    nodeVersion = "Version",
    nodeCoreVersion = "Core version",
    nodeMode = "Mode",
    nodeProtocol = "Protocol version",
    nodeStorage = "Storage",
    nodeHealth = "Health",
    ofdAuth = "BFD authorisation data",
    ofdNextReqNum = "Next request number",
    nodeUnknown = "The node did not answer",
    nodeAddress = "Node address",
    nodeAddressHint = "The node usually runs on this machine. A new address applies from the next request, no restart needed",
    workplace = "Workplace settings",
    mapServices = "Map services",
    mapServicesHint = "Until an address is set, the community map is used: it is not meant for every owner",
    mapTiles = "Map tiles",
    mapSearch = "Address search",
    mapReverse = "Address by marker",
    mapLocation = "Location lookup",
    mapDefault = "Back to community",
    groupAppearance = "Appearance and printing",
    groupService = "Service",
    groupIrreversible = "Irreversible",
    panelBehaviourHint = "What is chosen here is what the cashier sees when the sale screen opens. " +
        "They can still collapse or expand a section with the arrow on the screen itself.",
    panelPositionEntry = "New item",
    panelReceiptDetails = "Receipt details",
    panelMoney = "Payment and total",
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
    factoryHint = "The node issues the serial number by the manufacturer algorithm. " +
        "Take it together with the year to the BFD: they register the machine " +
        "and issue an identifier and a token.",
    ofdStep = "Step 2. Details from the BFD",
    localName = "Register name",
    localNameHint = "The name is kept on the node: every workplace and the sign-in " +
        "screen show it. Other register details come from the BFD and are not edited here.",
    save = "Save"
)

internal val englishPreview = PreviewStrings(
    title = "Print form",
    narrower = "Narrower",
    wider = "Wider",
    close = "Close",
    missing = "The node did not return a print form",
    print = "Print",
    printSent = "Sent to the printer",
    printFailed = "The printer refused the job",
    save = "Save to a file",
    saved = "Saved",
    zoomIn = "Zoom in",
    zoomOut = "Zoom out",
    fit = "Fit width",
    drawPin = "Cash register PIN",
    drawPinHint = "The print form is drawn by the cash register, and the node admits it by PIN. " +
        "The PIN stays in memory only and does not open the register sections.",
    draw = "Show the form",
    noDrawer = "The document came from the cabinet, but its print form is drawn by a cash " +
        "register on this machine. The node returned none, so there is nothing to draw with."
)

internal val englishStatus = StatusStrings(
    delivered = "Delivered",
    resent = "Sent later",
    refused = "Refused",
    internal = "Internal",
    queued = "Queued"
)

internal val englishEnums = EnumStrings(
    vatNone = "No VAT",
    vat0 = "VAT 0%",
    vat5 = "VAT 5%",
    vat10 = "VAT 10%",
    vat16 = "VAT 16%",
    paymentCash = "Cash",
    paymentCard = "Card",
    paymentElectronic = "Electronic",
    paymentMobile = "Mobile payment",
    paymentCredit = "On credit",
    paymentTare = "By tare",
    domainTrading = "Trading",
    domainServices = "Services",
    domainHotels = "Hotels",
    domainGasOil = "Petroleum products",
    domainTaxi = "Taxi",
    domainParking = "Parking",
    docCheck = "Receipt",
    docShiftOpen = "Shift opening",
    docShiftClose = "Shift closing",
    docCashIn = "Deposit",
    docCashOut = "Withdrawal",
    docReportX = "X report",
    stateActive = "In service",
    stateIdle = "Ready",
    stateBlocked = "Blocked",
    stateProgramming = "Programming",
    stateRegistration = "Registration"
)
