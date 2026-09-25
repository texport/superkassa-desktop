package kz.mybrain.superkassa.strings.impl.cabinet.applications

import kz.mybrain.superkassa.strings.api.cabinet.applications.ApplicationTexts

/** Надписи [ApplicationTexts] по-английски. */
internal val applicationTextsEn = ApplicationTexts(
    title = "KGD applications",
    registration = "Register",
    reregistration = "Re-register",
    deregistration = "Deregister",
    reason = "Reason",
    reasonCessation = "Not in use",
    reasonBroken = "Out of order",
    reasonLost = "Lost",
    reasonOther = "Other",
    comment = "Comment",
    newPlace = "New retail place",
    registerStatus = "Register status",
    submit = "File the application",
    sent = "Application sent",
    wait = "The KGD answers later — the journal below refreshes itself",
    failed = "The application was not sent",
    inFlight = "An application is already filed — the next one after the KGD reply",
    none = "The register is off the record — no applications are filed for it",
    stagePreparing = "Preparing the application",
    stageSigning = "Waiting for the signature in NCALayer",
    stageSending = "Sending to the cabinet",
    shiftOpenTitle = "The shift is open",
    shiftOpenAsk = "A register can be taken off record only with the shift closed. Close the shift on this register " +
        "and file the application?",
    shiftOpenElsewhere = "The shift is open and the register is set up on another machine — close the shift there",
    closeShiftAndDeregister = "Close the shift and take off record",
    adminPin = "Administrator PIN"
)
