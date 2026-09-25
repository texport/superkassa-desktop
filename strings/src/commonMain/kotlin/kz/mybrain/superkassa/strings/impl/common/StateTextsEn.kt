package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.EnumTexts
import kz.mybrain.superkassa.strings.api.common.PreviewTexts
import kz.mybrain.superkassa.strings.api.common.StatusTexts

/** Надписи [PreviewTexts] по-английски. */
internal val previewTextsEn = PreviewTexts(
    title = "Print form",
    narrower = "Narrower",
    wider = "Wider",
    close = "Close",
    missing = "The register did not draw the print form",
    print = "Print",
    printSent = "Sent to the printer",
    printFailed = "The printer refused the job",
    printerMissing = "This machine has no printer at all: there is nowhere to print",
    save = "Save to a file",
    saved = "Saved",
    zoomIn = "Zoom in",
    zoomOut = "Zoom out",
    fit = "Fit width",
    drawPin = "Cash register PIN",
    drawPinHint = "The print form is drawn by the cash register, which admits you by PIN. " +
        "The PIN stays in memory only and does not open the register sections.",
    draw = "Show the form",
    noDrawer = "The document came from the cabinet, but its print form is drawn by a cash " +
        "register on this machine. No register is set up here, so there is nothing to draw with."
)

/** Надписи [StatusTexts] по-английски. */
internal val statusTextsEn = StatusTexts(
    delivered = "Delivered",
    resent = "Sent later",
    refused = "Refused",
    internal = "Internal",
    queued = "Queued",
    unknown = "State unknown"
)

/** Надписи [EnumTexts] по-английски. */
internal val enumTextsEn = EnumTexts(
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
    docSale = "Sale",
    docReturn = "Sale return",
    docBuy = "Purchase",
    docBuyReturn = "Purchase return",
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
