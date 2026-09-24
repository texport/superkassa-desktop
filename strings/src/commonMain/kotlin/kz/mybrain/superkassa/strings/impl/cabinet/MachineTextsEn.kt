package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.MachineTexts

/** Надписи [MachineTexts] по-английски. */
internal val machineTextsEn = MachineTexts(
    title = "Work on this machine",
    worksHere = "The register is set up on this machine",
    goToKkm = "Go to the register",
    onlyOnRecord = "Only a register on record with the KGD can be worked from",
    notHere = "The register is not set up on this machine",
    workHere = "Work on this machine",
    tokenReissued = "The token will be issued anew: the cabinet cannot show the current one. " +
        "The previous token stops working.",
    heardByOfd = "The BFD has received data from this register",
    handoverUnderstood = "I understand: on the other machine this register will stop working",
    done = "The register is set up — the cashier signs in with this administrator PIN",
    stranded = "The token was issued, but the register was not set up on this machine. Retry: " +
        "the same token is sent again, it is not issued twice.",
    retry = "Retry"
)
