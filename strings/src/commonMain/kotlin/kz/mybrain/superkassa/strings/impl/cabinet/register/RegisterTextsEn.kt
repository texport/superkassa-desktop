package kz.mybrain.superkassa.strings.impl.cabinet.register

import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterTexts

/** Надписи [RegisterTexts] по-английски. */
internal val registerTextsEn = RegisterTexts(
    passport = "Register passport",
    internalName = "Register name",
    factoryNumber = "Factory number",
    factoryLocked = "Recorded in the registration card — changed only by re-registration",
    manufactureYear = "Year of manufacture",
    model = "Model",
    registrationNumber = "Registration number",
    status = "Status",
    technicalState = "Technical state",
    technicalUnknown = "The BFD has not seen this register yet",
    trafficSuspended = "Document reception is suspended",
    bfdDisconnected = "The link to the BFD is broken",
    shift = "Shift",
    lastContact = "Last contact",
    lastContactNever = "the register has never made contact",
    shiftNumberTitle = "Shift number",
    shiftNumberNone = "there have been no shifts yet",
    token = "Token",
    issueToken = "Issue a token",
    tokenIssued = "Token issued",
    tokenGoesToNode = "The new token has been written into the register on this machine",
    tokenUnconfirmed = "The BFD intake service has not confirmed the new token yet — " +
        "issue it again in a minute",
    tokenNeedsNode = "The register works on this machine: write the new token into its settings, otherwise it stops " +
        "at the first receipt",
    tokenOnlyRegistered = "A token is issued to a register on record, and only when no application is filed for it",
    localInfoSynced = "The register details on this machine have been refreshed",
    localInfoNeedsSync = "The new place reaches receipts after the details are synced in the register settings: it " +
        "cannot be done right now",
    delete = "Delete the register",
    deleteOnlyDraft = "Only a register that is not on record yet can be deleted",
    actionsJournal = "Action journal",
    actionsEmpty = "No registration actions yet",
    state = registerStateTextsEn,
    card = registrationCardTextsEn
)
