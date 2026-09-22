package kz.mybrain.superkassa.desktop.ui.strings

/** Надписи торговой сводки по-английски. Состав полей задан в [AnalyticsSalesTexts]. */
private val analyticsSalesTextsEn = AnalyticsSalesTexts(
    tab = "Sales",
    forPeriod = "Summary for",

    overview = "Executive summary",
    overviewHint = "The network's key figures for the period and how they changed against the " +
        "previous period of the same length. The cashless share is counted from the payment " +
        "type amounts: card, electronic money and mobile payment",
    vat = "VAT",
    cashless = "Cashless share",
    versusPrevious = "vs the previous period",
    percentPoints = "pp",
    online = "Registers in contact",
    silent = "Silent for the period",

    revenue = "Revenue",
    receipts = "Receipts",
    average = "Average receipt",
    refunds = "Refunds",
    tax = "Tax",
    net = "Net",

    empty = "No documents for this period",
    emptyHint = "Take a wider period — a week or a month — or page it with the arrows",

    openShifts = "Open shifts",
    offline = "Offline",
    queuedCount = "Awaiting reply",
    unknownCount = "No information",

    paidOut = "Paid out",
    purchasesHint = "What the register buys from individuals is paid out of the drawer: this money is not revenue and is not part of the numbers above",

    byDay = "Revenue by day",
    byHour = "Load by hour",
    payments = "Payment types",
    paymentOther = "Other",
    noPayments = "No payments for this period",
    nothingToDraw = "Nothing to draw for this period",

    registers = "Cash registers",
    places = "Retail places",
    allRegistersShown = "All cash registers shown",
    allPlacesShown = "All retail places shown",
    colName = "Name",

    regions = "By region",
    regionsHint = "Network revenue by the regions of the retail places: the region is taken " +
        "from the place address. The network share shows how much of all revenue fell to the region",
    region = "Region",
    placeCount = "Places",
    activeRegisters = "Registers with receipts",
    networkShare = "Network share",
    noAddress = "No address",

    delivery = "Document delivery",
    delivered = "Delivered",
    queued = "In queue",
    unknown = "No information",
    rejected = "Rejected",

    byDayHint = "Revenue by the days of the period: it shows which days feed you and which " +
        "stand idle. An empty day is either a day off or a register that never connected",
    byHourHint = "When people come: these hours are what the shifts and the cashier's lunch " +
        "are planned by. The hours are counted by the register's clock, not by this one",
    paymentsHint = "What people paid with: the shares of cash, cards and the other payment " +
        "types. They show whether the terminal agrees with the register and how much money " +
        "settled in the drawer",
    registersHint = "How much each register sells: the rows show which one works and which " +
        "one is silent. A silent register is either a closed place or a lost connection",
    placesHint = "The same by retail place: a place's revenue is collected from all of its " +
        "registers. Places are compared with each other — the total revenue is fully here",
    deliveryHint = "Whether the documents reached the BFD. The rejected ones and the ones " +
        "with no information are receipts the state may not have: they are dealt with at " +
        "once, not at the end of the month"
)

/** Надписи аналитики по-английски. Состав полей задан в [AnalyticsTexts]. */
internal val analyticsTextsEn = AnalyticsTexts(
    title = "Analytics",
    mapTab = "Cash registers on the map",
    exchangeTab = "Exchange addresses",

    positionSource = "Take the position",
    sourceAddress = "By retail place address",
    sourceCabinet = "By cabinet",
    sourceKkm = "By cash register",
    sourceAddressHint = "Where the register is recorded with the KGD: the retail place address. " +
        "The address registry holds no coordinates — the map finds the building",
    sourceCabinetHint = "Where the owner placed the point when creating the retail place",
    sourceKkmHint = "Where the register believes it stands: the machine sent the coordinates itself",

    refresh = "Refresh",
    unreachable = "The cabinet does not answer",
    unreachableHint = "Check the cabinet address in the settings and the connection, then refresh",
    notDeployed = "The cabinet has no analytics yet",
    notDeployedHint = "The cabinet answered that it does not know this section: it is not deployed yet. " +
        "The screen will work on its own once the deployment is done",
    refused = "The cabinet refused",

    placed = "On the map",
    withoutPosition = "Without a position",
    kkmCount = "Registers",
    kkmColumn = "Register",
    addressCount = "Addresses",

    mapEmpty = "No cash register on the map",
    mapEmptyHint = "With this position source there is nothing to place — see the list next to the map",
    mapSearching = "Looking up register addresses on the map",
    mapSearchingHint = "The cabinet gave retail place addresses without coordinates — " +
        "points appear as the lookup goes",
    pickPin = "Pick a cash register on the map",
    pickPinHint = "Click a point — its card appears here",
    mapFullscreen = "Full screen",
    mapFullscreenExit = "Exit full screen",
    mapShown = "Visible on the map",
    mapShownOf = "%s of %s registers",
    mapOnRecordOf = "On the record: %s of %s",
    mapSievedOf = "The filter left %s of %s",
    mapLegend = "Map legend",
    legendGood = "Registers here are on the record",
    legendIdle = "None on the record yet",
    legendSomeTrouble = "Some blocked or refused",
    legendTrouble = "Many blocked or refused",
    legendSize = "A bigger circle means more registers",
    legendChosen = "The chosen place is filled in",

    searchKkm = "A name, a KGD number or an address",
    searchKkmLabel = "Find a cash register",
    allPlaces = "All retail places",
    markShiftOpen = "Shift open",
    markBlocked = "Blocked",
    allRecords = "Any record state",
    markOnRecord = "On the record",
    markEntered = "Added in the cabinet",
    markApplied = "Filed with the KGD",
    markRefused = "Refused by the KGD",
    markDeregistered = "Struck off the record",
    sieveEmpty = "No cash register matches the filter",
    sieveEmptyHint = "Clear some of the filter chips or change the search line",
    sieveClear = "Clear the filter",
    kkmsHere = "Cash registers here",
    openKkmSales = "Cash register analytics",
    kkmSalesTitle = "What this cash register sells",

    kkmListEmpty = "No registers in the cabinet",
    kkmListEmptyHint = "Add a register on the retail places screen and file a KGD application to put it on record",
    reasonNoAddress = "The retail place has no address chosen",
    reasonNoCabinetPoint = "The cabinet holds no coordinates for the retail place",
    reasonNoKkmPoint = "The register has never sent its coordinates",
    reasonSearching = "Looking for the address on the map",
    reasonNotOnMap = "The map did not find this address",

    registrationNumber = "KGD number",
    noRegistrationNumber = "No KGD number assigned",
    retailPlace = "Retail place",
    blocked = "Blocked",
    lastContact = "Last contact",
    neverSeen = "No contact yet",
    positionFrom = "Position",
    fromAddress = "By the retail place address",
    fromCabinet = "By the cabinet coordinates",
    fromKkm = "By the register coordinates",
    geoSource = "How it was determined",

    exchangeTitle = "Addresses the registers sent data from",
    exchangeHint = "A service detail: it shows that a register connects from somewhere other than " +
        "where it is recorded, and that several machines work behind one address",
    exchangeEmpty = "No exchange yet",
    exchangeEmptyHint = "The address appears here after the register first contacts BFD",
    exchangeNotFound = "Nothing found",
    exchangeNotFoundHint = "Change the search text or drop the register filter",
    search = "Search by address or register",
    allRegisters = "All registers",
    exchangeAddress = "Address",
    firstSeen = "First seen",
    lastSeen = "Last seen",

    sales = analyticsSalesTextsEn,
    record = analyticsRecordTextsEn
)
