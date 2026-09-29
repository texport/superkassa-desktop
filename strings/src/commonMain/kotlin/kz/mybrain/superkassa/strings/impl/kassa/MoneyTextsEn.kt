package kz.mybrain.superkassa.strings.impl.kassa

import kz.mybrain.superkassa.strings.api.kassa.CashierTexts
import kz.mybrain.superkassa.strings.api.kassa.DrawerTexts
import kz.mybrain.superkassa.strings.api.kassa.KkmSetupTexts
import kz.mybrain.superkassa.strings.api.kassa.MoneyTexts

/** Надписи [MoneyTexts] по-английски. */
internal val moneyTextsEn = MoneyTexts(
    drawer = DrawerTexts(
        inDrawer = "In the cash drawer",
        countedByNode = "The balance is counted by the cash register: it also decides whether a payout fits.",
        unknownBalance = "The cash register did not report the balance — it will check the payout itself",
        shiftClosed = "The shift is closed. Cash can be paid in and out only while a shift is open.",
        kkmBlocked = "The register is blocked and performs no cash movements.",
        shiftClosedShort = "Shift is closed",
        amountHint = "For example, 1 500.00",
        notANumber = "This is not an amount. Enter a number, for example 1 500.00",
        notPositive = "The amount must be greater than zero",
        tooLarge = "The amount exceeds what the register handles in one operation",
        notEnough = "The drawer holds only %s — there is nothing more to take out",
        confirmDeposit = "Pay in %s?",
        confirmWithdraw = "Take out %s?",
        afterOperation = "The drawer will hold %s",
        confirm = "Confirm",
        cancel = "Cancel",
        working = "Processing…",
        recent = "Recent pay-ins and payouts",
        recentHint = "Cash put in and taken out over the last day. They are not revenue, but they change what is in " +
            "the drawer — the closing reconciliation rests on them",
        recentEmpty = "No pay-ins or payouts in the last day",
        recentEmptyHint = "Cash paid in and taken out appears here as soon as it goes through.",
        recentUnread = "The cash movements could not be read: the cash register did not answer.",
        recentUnreadHint = "Read them again: while the cash register is silent, nothing is known " +
            "about this day's pay-ins and payouts."
    ),
    cashiers = CashierTexts(
        addTitle = "New cashier",
        listTitle = "Cashiers of this register",
        listHint = "The cashiers of this cash register: who may stand at it and what they are allowed to do. A cashier PIN " +
            "is their signature on a receipt, and only they should know it",
        pinLength = "4–10 digits",
        pinUnique = "The cash register will not give two of its cashiers the same PIN: it tells them apart by PIN.",
        roles = "An administrator adds cashiers and changes settings, a cashier issues receipts.",
        ownPin = "Your own PIN is changed right here: work continues with the new PIN, no need to sign in again.",
        pinTooShort = "The PIN has fewer than four digits",
        pinTooLong = "The PIN has more than ten digits",
        nameRequired = "Enter the cashier name",
        deleteConfirm = "Delete %s?",
        deleteExplain = "The cashier loses access to the register. The receipts they issued stay in place.",
        deleteBlocked = "The only administrator cannot be deleted: no one would be left to add cashiers and change settings. Add a second administrator first, then delete.",
        changePinFor = "New PIN for %s",
        onlyInRole = "The only administrator",
        empty = "No cashiers have been added to this register",
        emptyHint = "Add the first one — they will appear in this list.",
        unreadable = "The cash register did not return the cashier list",
        unreadableHint = "Try again; if it fails, call service."
    ),
    kkm = KkmSetupTexts(
        stepOne = "Step 1. Factory number and year of manufacture",
        stepTwo = "Step 2. Identifier and token from the BFD",
        copy = "Copy",
        copied = "Copied",
        factoryReady = "The number is ready. Take it to the BFD office and get an identifier and a token.",
        renameSaved = "The name is saved in the register",
        renameReset = "Clear the name",
        diagnosticsHint = "The checks change nothing in the register: they ask the BFD how things stand.",
        diagnosticsEmpty = "No checks have been run yet",
        programmingOn = "Programming mode is on",
        programmingOff = "Normal mode",
        ofdEmpty = "The BFD sent no details",
        ofdAnswer = "BFD answer",
        ofdKgdNumber = "State Revenue Committee number",
        ofdFactoryNumber = "Factory number",
        ofdSystemId = "Identifier at the BFD",
        ofdProtocol = "Protocol version",
        ofdOrganization = "Organisation",
        ofdAddress = "Installation address",
        ofdBin = "BIN",
        bfdProtocol = "BFD protocol",
        bfdTimeout = "BFD answer timeout, s",
        kassaStorage = "Storage",
        storageLocal = "Database on this machine",
        storageServer = "Database server",
        bfdMeaning = "BFD is the fiscal data base: receipts go there and register details " +
            "come from there.",
        syncTitle = "Synchronisation with the BFD",
        syncService = "Refresh register details",
        syncServiceHint = "Organisation, address and registration numbers come from the BFD. " +
            "A closed shift and an empty queue are required.",
        syncServiceDone = "Register details refreshed from the BFD",
        syncCounters = "Refresh counters",
        syncCountersHint = "The register pulls counters and the shift number from the BFD. The queue must be empty.",
        syncCountersDone = "Counters refreshed from the BFD",
        decommission = "Remove the register from this workplace",
        decommissionHint = "The register is deleted from this machine together with its documents. " +
            "The KGD record stays as it is: a register is deregistered only by an application from the cabinet. " +
            "This cannot be undone.",
        needProgramming = "The register is in programming mode",
        needShiftClosed = "The shift is closed",
        needQueueEmpty = "The delivery queue is empty",
        needOnline = "Autonomous mode is off",
        doProgramming = "Enter programming mode",
        doShiftClosed = "Close the shift",
        doQueueEmpty = "Send the queue to BFD",
        doOnline = "Leave autonomous mode",
        decommissionConfirm = "Remove the register %s from this machine?",
        decommissionDone = "The register has been removed from this workplace"
    )
)
