package kz.mybrain.superkassa.strings.impl.cabinet.register

import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterStateTexts

/** Надписи [RegisterStateTexts] по-английски. */
internal val registerStateTextsEn = RegisterStateTexts(
    disagree = "They disagree",
    disagreeNote = "They disagree: %s. Most likely the state went stale somewhere — reread the register " +
        "card; if the disagreement stays, work out who is right",
    sourceNode = "This machine",
    sourceCabinet = "Cabinet",
    sourceBfd = "BFD",
    working = "The register is in service",
    blocked = "The register is blocked",
    offRecord = "The register is off record",
    workUnknown = "Nobody said whether the register works",
    shiftUnknown = "Nobody knows whether the shift is open",
    claimWorking = "the register is in service",
    claimBlocked = "the register is blocked",
    claimOnRecord = "the register is on record",
    claimOffRecord = "the register is off record",
    claimRecordUnread = "the record was not read",
    claimNotFiled = "never filed for record",
    claimIsnaPending = "KGD is reviewing the application",
    claimIsnaRefused = "KGD refused the record",
    claimNodeNoKkm = "does not know this register",
    claimBfdNoKkm = "has not seen the register yet",
    claimBfdNoAnswer = "did not answer about the register",
    claimShiftOpen = "the shift is open",
    claimShiftClosed = "the shift is closed",
    claimShiftNotKept = "does not keep shifts"
)
