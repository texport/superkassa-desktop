package kz.mybrain.superkassa.strings.impl.kassa.checkout

import kz.mybrain.superkassa.strings.api.kassa.checkout.CheckoutTexts

/** Надписи [CheckoutTexts] по-английски. */
internal val checkoutTextsEn = CheckoutTexts(
    toPay = "To pay",
    issued = "Receipt issued",
    issuedSum = "Receipt total",
    change = "Change",
    showReceipt = "Show receipt",
    printReceipt = "Print",
    nextReceipt = "Next receipt",
    confirmGive = "Give %s back to the customer?",
    confirmTake = "Take %s back from the seller?",
    confirmBasis = "Against receipt No. %s for %s. The refund goes to the BFD and cannot be undone.",
    confirmCancel = "Cancel",
    refundDone = "%s for %s",
    refundPart = "Less than the receipt refunds a part"
)
