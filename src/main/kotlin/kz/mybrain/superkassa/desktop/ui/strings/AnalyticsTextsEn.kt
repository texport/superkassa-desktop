package kz.mybrain.superkassa.desktop.ui.strings

/** Надписи торговой сводки по-английски. Состав полей задан в [AnalyticsSalesTexts]. */
private val analyticsSalesTextsEn = AnalyticsSalesTexts(
    tab = "Sales",
    forPeriod = "Summary for",

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

    delivery = "Document delivery",
    delivered = "Delivered",
    queued = "In queue",
    unknown = "No information",
    rejected = "Rejected"
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
    pickPin = "Pick a cash register on the map",
    pickPinHint = "Click a point — its card appears here",

    searchKkm = "A name, a KGD number or an address",
    allPlaces = "All retail places",
    markShiftOpen = "Shift open",
    markBlocked = "Blocked",
    markOffRecord = "Off the record",
    sieveEmpty = "No cash register matches the filter",
    sieveEmptyHint = "Clear some of the filter chips or change the search line",
    sieveClear = "Clear the filter",
    kkmsHere = "Cash registers here",
    openKkmSales = "Cash register analytics",
    kkmSalesTitle = "What this cash register sells",

    withoutPositionEmpty = "Every register is on the map",
    withoutPositionEmptyHint = "With this position source the place of every register is known",
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
    exchangeEmptyHint = "The address appears here after the register first contacts the receiving service",
    exchangeNotFound = "Nothing found",
    exchangeNotFoundHint = "Change the search text or drop the register filter",
    search = "Search by address or register",
    allRegisters = "All registers",
    exchangeAddress = "Address",
    firstSeen = "First seen",
    lastSeen = "Last seen",

    sales = analyticsSalesTextsEn
)
