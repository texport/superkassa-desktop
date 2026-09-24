package kz.mybrain.superkassa.strings.impl.kassa.refusal

import kz.mybrain.superkassa.strings.api.kassa.refusal.KassaRefusalTexts

/** Надписи [KassaRefusalTexts] по-английски. */
internal val kassaRefusalTextsEn = KassaRefusalTexts(
    programming = "The register is in programming mode: leave it in the register settings",
    vatNotPayer = "The register is not a VAT payer: set the items to “No VAT”",
    vatUnknown = "The register does not know this VAT rate: choose a rate from the list",
    paymentUnsupported = "The register does not accept this payment type now: choose another one",
    unitUnknown = "The register does not know this unit: choose a unit from the list",
    outOfRange = "A price, quantity or discount is out of range: check the items and the payment",
    basisRequired = "Choose the original receipt for the refund",
    unknown = "The register refused, refusal code %s"
)
