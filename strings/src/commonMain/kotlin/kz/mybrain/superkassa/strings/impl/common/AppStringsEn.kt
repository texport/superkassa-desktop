package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.common.CommonStrings
import kz.mybrain.superkassa.strings.api.common.LoginStrings
import kz.mybrain.superkassa.strings.api.common.SectionStrings
import kz.mybrain.superkassa.strings.api.common.ShellStrings

/** Надписи [AppStrings] по-английски. */
internal val appStringsEn = AppStrings(
    common = CommonStrings(
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
        collapse = "Collapse",
        explain = "Explanation",
        expand = "Expand",
        retry = "Try again",
        nothingToPick = "Nothing to choose from"
    ),
    login = LoginStrings(
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
    shell = ShellStrings(
        noKkm = "No register chosen",
        autonomous = "Autonomous mode",
        blocked = "Blocked",
        changeCashier = "Change cashier",
        moreActions = "More"
    ),
    sections = SectionStrings(
        dashboard = "Overview",
        sale = "Sale",
        returns = "Refund",
        cash = "Cash",
        history = "History",
        queue = "Queue",
        users = "Cashiers",
        settings = "Settings",
        register = "New register",
        cabinet = "BFD cabinet"
    ),
    dashboard = dashboardStringsEn,
    autonomous = autonomousStringsEn,
    sale = saleStringsEn,
    returns = returnStringsEn,
    cash = cashStringsEn,
    queue = queueStringsEn,
    users = userStringsEn,
    settings = settingStringsEn,
    preview = previewStringsEn,
    status = statusStringsEn,
    enums = enumStringsEn
)
