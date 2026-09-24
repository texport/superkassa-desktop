package kz.mybrain.superkassa.strings.impl.kassa.checkout

import kz.mybrain.superkassa.strings.api.kassa.checkout.CheckoutTexts

/** Надписи [CheckoutTexts] по-казахски. */
internal val checkoutTextsKk = CheckoutTexts(
    toPay = "Төлеуге",
    issued = "Чек басылды",
    issuedSum = "Чек сомасы",
    change = "Қайтарым",
    showReceipt = "Чекті көрсету",
    printReceipt = "Басып шығару",
    nextReceipt = "Келесі чек",
    confirmGive = "Сатып алушыға %s қайтарылсын ба?",
    confirmTake = "Сатушыдан %s қабылдансын ба?",
    confirmBasis = "№ %s чек бойынша, сомасы %s. Қайтару БФД-ға жіберіледі және болдырылмайды.",
    confirmCancel = "Бас тарту",
    refundDone = "%s: %s",
    refundPart = "Чек сомасынан аз — бөлігі қайтады"
)
