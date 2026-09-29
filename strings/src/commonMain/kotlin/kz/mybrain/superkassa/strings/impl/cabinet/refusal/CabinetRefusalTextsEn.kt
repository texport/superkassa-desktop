package kz.mybrain.superkassa.strings.impl.cabinet.refusal

import kz.mybrain.superkassa.strings.api.cabinet.refusal.CabinetRefusalTexts

/** Надписи [CabinetRefusalTexts] по-английски. */
internal val cabinetRefusalTextsEn = CabinetRefusalTexts(
    unreachable = "The cabinet does not answer at the configured address",
    unreadable = "The cabinet answered in a form the app cannot read",
    sessionExpired = "Access expired — sign in again",
    noNcaLayer = "NCALayer does not answer. Start it and sign in again",
    signDeclined = "No signature received",
    signWindowClosed = "The signing window was closed",
    signCancelled = "Signing was cancelled in NCALayer",
    kkmNotActive = "The register is not admitted to service at the acceptance server",
    defaultPinNotAllowed = "The register still has the default PIN — change it on the register itself",
    accessDenied = "The BFD cabinet is closed to this company — contact BFD to open access"
)
