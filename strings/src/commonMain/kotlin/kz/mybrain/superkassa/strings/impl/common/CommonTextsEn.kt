package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.common.GeneralTexts
import kz.mybrain.superkassa.strings.api.common.LoginTexts
import kz.mybrain.superkassa.strings.api.common.SectionTexts
import kz.mybrain.superkassa.strings.api.common.StatusHints
import kz.mybrain.superkassa.strings.api.common.TopBarTexts
import kz.mybrain.superkassa.strings.impl.share.shareTextsEn

/** Надписи [CommonTexts] по-английски. */
internal val commonTextsEn = CommonTexts(
    general = GeneralTexts(
        refresh = "Refresh",
        hide = "Hide",
        print = "Print",
        amount = "Amount",
        pin = "PIN",
        loading = "Loading…",
        starting = "Register is starting",
        noAnswer = "No answer received — the document may have gone through, check the journal; " +
            "a repeat will not duplicate it",
        kassaFailed = "The cash register could not complete the action — try again; if it fails, call service",
        refusalCode = "Refusal code",
        deliveredToOfd = "delivered to the BFD",
        queuedNoLink = "no link — queued",
        deliveryState = "Delivery state",
        notAccepted = "%1\$s, but the BFD did not accept it: %2\$s",
        collapse = "Collapse",
        explain = "Explanation",
        expand = "Expand",
        retry = "Try again",
        nothingToPick = "Nothing to choose from"
    ),
    login = LoginTexts(
        title = "Sign in to the register",
        search = "Search: number, name, company",
        noKkmsTitle = "No registers at this workplace",
        noKkms = "No register has been added here yet. Add one or start from the cabinet.",
        kkmsUnreadTitle = "The register list was not read",
        kkmsUnread = "The register list could not be read, so how many registers are here is unknown. " +
            "Try again; if it fails, call service.",
        yourKkm = "Your register",
        pick = "Choose",
        picked = "Chosen",
        enter = "Sign in",
        reload = "Reload list",
        noKkmChosen = "No register chosen",
        pickHint = "Choose a register from the list",
        factory = "Serial",
        registrationNumber = "Reg. no."
    ),
    topBar = TopBarTexts(
        noKkm = "No register chosen",
        autonomous = "Autonomous mode",
        blocked = "Blocked",
        changeCashier = "Change cashier",
        statusHints = StatusHints(
            active = "The register is active: it issues receipts and sends them to the BFD.",
            blocked = "The register is blocked: the BFD does not accept its receipts. See the reason " +
                "and how to unblock it in the BFD cabinet.",
            programming = "The register is in programming mode: receipts cannot be issued while it is on. " +
                "Turn it off in the register settings, “General”.",
            registration = "The register is not yet registered with the KGD: receipts cannot be issued. " +
                "Submit an application in the BFD cabinet.",
            autonomous = "No connection to the BFD: receipts are issued and queued, and the register sends " +
                "them itself once the connection is back.",
            shiftOpen = "The shift is open: you can issue receipts. Close it with a Z report on the home " +
                "screen before the day ends.",
            shiftClosed = "The shift is closed: open it on the home screen to issue receipts."
        ),
        moreActions = "More"
    ),
    sections = SectionTexts(
        dashboard = "Overview",
        sale = "Sale",
        returns = "Refund",
        cash = "Cash",
        history = "History",
        queue = "Queue",
        users = "Cashiers",
        settings = "Settings",
        register = "New register",
        cabinet = "BFD cabinet",
        kkms = "Registers",
        menu = "Sections"
    ),
    dashboard = dashboardTextsEn,
    autonomous = autonomousTextsEn,
    receipt = receiptTextsEn,
    returns = returnTextsEn,
    cash = cashTextsEn,
    queue = queueTextsEn,
    users = userTextsEn,
    settingsScreen = settingsScreenTextsEn,
    preview = previewTextsEn,
    share = shareTextsEn,
    status = statusTextsEn,
    enums = enumTextsEn
)
