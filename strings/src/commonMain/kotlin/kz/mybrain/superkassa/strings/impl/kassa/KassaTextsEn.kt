package kz.mybrain.superkassa.strings.impl.kassa

import kz.mybrain.superkassa.strings.api.kassa.BlockReasonTexts
import kz.mybrain.superkassa.strings.api.kassa.KassaTexts
import kz.mybrain.superkassa.strings.api.kassa.PaymentTexts
import kz.mybrain.superkassa.strings.api.kassa.UnitTexts
import kz.mybrain.superkassa.strings.impl.kassa.checkout.checkoutTextsEn
import kz.mybrain.superkassa.strings.impl.kassa.contact.buyerContactTextsEn
import kz.mybrain.superkassa.strings.impl.kassa.refusal.kassaRefusalTextsEn

/** Надписи [KassaTexts] по-английски. */
internal val kassaTextsEn = KassaTexts(
    money = moneyTextsEn,
    sale = saleTextsEn,
    payment = PaymentTexts(
        addPayment = "Add payment",
        removePayment = "Remove payment",
        amount = "Amount",
        rest = "Rest of the total — the register counts it",
        splitEmpty = "Enter the amount of every payment except the last",
        splitExcess = "Payments add up to more than the receipt total",
        rejectedKept = "What you entered is still on screen: correct it and submit again"
    ),
    blockReason = BlockReasonTexts(
        unknown = "The register is blocked",
        invalidToken = "The register token is invalid: issue a new one in the cabinet and enter it in the settings",
        shiftTooLong = "The shift has been open for over a day: close it with a Z-report",
        autonomousTooLong = "Autonomous work lasted too long: restore the connection to the BFD",
        deregistered = "The register is deregistered",
        disconnected = "The register is disconnected from the BFD",
        incorrectData = "The BFD rejected the register data",
        readingStays = "A blocked register stays readable: journal, shifts and reports."
    ),
    units = UnitTexts(
        mapOf(
            "796" to "pcs",
            "116" to "kg",
            "5114" to "svc",
            "006" to "m",
            "112" to "L",
            "021" to "lin. m",
            "168" to "t",
            "356" to "h",
            "359" to "day",
            "360" to "wk",
            "362" to "mo",
            "003" to "mm",
            "004" to "cm",
            "005" to "dm",
            "642" to "unit",
            "008" to "km",
            "160" to "hg",
            "161" to "mg",
            "162" to "ct",
            "163" to "g",
            "164" to "µg",
            "110" to "mm³",
            "111" to "mL",
            "055" to "m²",
            "059" to "ha",
            "061" to "km²",
            "625" to "sheet",
            "728" to "pack",
            "736" to "roll",
            "778" to "pkg",
            "868" to "btl",
            "931" to "job",
            "113" to "m³"
        )
    ),
    contact = buyerContactTextsEn,
    checkout = checkoutTextsEn,
    refusal = kassaRefusalTextsEn
)
