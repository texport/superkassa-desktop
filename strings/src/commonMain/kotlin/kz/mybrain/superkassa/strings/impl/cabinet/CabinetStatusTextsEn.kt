package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetStatusTexts

/** Надписи [CabinetStatusTexts] по-английски. */
internal val cabinetStatusTextsEn = CabinetStatusTexts(
    draft = "Draft",
    registered = "On record",
    deregistered = "Deregistered",
    active = "Working",
    inactive = "Stopped",
    blocked = "Blocked",
    accepted = "Accepted",
    rejected = "Refused",
    sent = "Sent",
    inProcess = "The KGD is reviewing",
    shiftOpen = "Shift is open",
    shiftClosed = "Shift is closed",
    unknown = "State is unknown"
)
