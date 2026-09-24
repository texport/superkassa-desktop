package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.AutonomousStrings
import kz.mybrain.superkassa.strings.api.common.CashStrings
import kz.mybrain.superkassa.strings.api.common.DashboardStrings
import kz.mybrain.superkassa.strings.api.common.ReturnStrings
import kz.mybrain.superkassa.strings.api.common.SaleStrings

/** Надписи [DashboardStrings] по-английски. */
internal val dashboardStringsEn = DashboardStrings(
    state = "State",
    shift = "Shift",
    shiftOpenNo = "No. %s open",
    shiftClosed = "Closed",
    documentsInShift = "Documents in shift",
    shiftDocuments = "Shift documents",
    refused = "Refused by the BFD",
    refusedHint = "The BFD did not accept these documents: they are not fiscal, they are outside the shift " +
        "counters and they have no printed form. The refusal code and the cashier are for support.",
    openShift = "Open shift",
    xReport = "X report",
    closeShift = "Close shift",
    closeShiftAsk = "Close the shift and take the Z report?",
    closeShiftExplain = "Documents in the shift: %s, %s in the drawer. The Z report goes to BFD and cannot be undone.",
    closeShiftCashout = "The remaining cash is withdrawn together with the report.",
    closeShiftKeepsCash = "The remaining cash stays in the drawer and carries over to the next shift.",
    shiftTooLong = "The shift has been open for over a day: close it with a Z-report or the BFD blocks the register.",
    shiftDayLimit = "Close the shift by %s: after that the register refuses receipts, returns and cash moves.",
    openShiftHint = "The shift is closed — open it to issue receipts",
    openShiftAdmin = "Only an administrator opens the shift — call them to start the day.",
    shiftOpened = "Shift opened",
    shiftClosedDone = "Shift closed, Z report sent",
    xReportDone = "X report produced",
    printForm = "Print form",
    shiftUnknown = "Unknown",
    shiftEmpty = "No documents yet",
    shiftEmptyHint = "The shift is open: the first receipt shows up here right after it goes through.",
    documentsUnread = "The shift documents could not be read",
    documentsUnreadHint = "The shift is open but the cash register did not hand over its documents: " +
        "how many there are is unknown.",
    shiftUnknownHint = "The cash register did not report the shift state — refresh it; " +
        "if that does not help, call service.",
    documentNo = "No."

)

/** Надписи [AutonomousStrings] по-английски. */
internal val autonomousStringsEn = AutonomousStrings(
    title = "Autonomous mode",
    explain = "There is no link to the BFD. Receipts are issued with an autonomous sign " +
        "and go to the BFD by themselves once the link is back. You can keep working.",
    waiting = "Waiting to be sent",
    checkLink = "Check the link",
    sendQueued = "Send what piled up",
    sendQueuedRules = "Only an admin can flush the queue: the shift must be closed and the register in programming mode.",
    linkBack = "The link to the BFD is back"
)

/** Надписи [SaleStrings] по-английски. */
internal val saleStringsEn = SaleStrings(
    receipt = "Receipt",
    sale = "Sale",
    purchase = "Purchase",
    issueSale = "Issue receipt",
    issuePurchase = "Issue purchase",
    issuing = "Issuing…",
    name = "Name",
    price = "Price",
    quantity = "Quantity",
    measureUnit = "Unit of measure",
    vat = "VAT",
    discount = "Discount",
    storno = "Storno",
    stornoUndo = "Undo storno",
    add = "Add",
    remove = "Remove",
    clearBasket = "Clear basket",
    receiptDiscount = "Receipt discount",
    receiptMarkup = "Receipt markup",
    discountOrMarkup = "Receipt discount and markup — one or the other",
    customerBin = "Customer IIN/BIN",
    taken = "Tendered",
    total = "Total",
    barcode = "Barcode",
    barcodeSearch = "Search by barcode",
    barcodeSearching = "Searching…",
    barcodeFind = "Find",
    barcodeMissing = "No such barcode in the catalogue — add the item by hand.",
    barcodeUnavailable = "The catalogue is unavailable right now — add the item by hand.",
    payment = "Payment type",
    delivered = "receipt issued and delivered to the BFD",
    queued = "receipt issued, no connection — queued"
)

/** Надписи [ReturnStrings] по-английски. */
internal val returnStringsEn = ReturnStrings(
    title = "Refund",
    empty = "The current shift has no receipt to refund against.",
    receiptNo = "Receipt No.",
    refundFor = "Refund for receipt No.",
    saleReturn = "Sale refund",
    giveBack = "Give back to customer",
    purchaseReturn = "Purchase refund",
    takeBack = "Take back",
    done = "issued, state"
)

/** Надписи [CashStrings] по-английски. */
internal val cashStringsEn = CashStrings(
    deposit = "Deposit",
    withdraw = "Withdraw",
    deposited = "Deposited",
    withdrawn = "Withdrawn"
)
