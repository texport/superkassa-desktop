package kz.mybrain.superkassa.strings.impl.kassa

import kz.mybrain.superkassa.strings.api.kassa.BlockReasonTexts
import kz.mybrain.superkassa.strings.api.kassa.KassaTexts
import kz.mybrain.superkassa.strings.api.kassa.PaymentTexts
import kz.mybrain.superkassa.strings.api.kassa.UnitTexts
import kz.mybrain.superkassa.strings.impl.kassa.checkout.checkoutTextsKk
import kz.mybrain.superkassa.strings.impl.kassa.contact.buyerContactTextsKk
import kz.mybrain.superkassa.strings.impl.kassa.refusal.kassaRefusalTextsKk

/** Надписи [KassaTexts] по-казахски. */
internal val kassaTextsKk = KassaTexts(
    money = moneyTextsKk,
    sale = saleTextsKk,
    payment = PaymentTexts(
        addPayment = "Төлем қосу",
        removePayment = "Төлемді алып тастау",
        amount = "Сома",
        rest = "Қалдық",
        splitEmpty = "Соңғысынан басқа әр төлемнің сомасын көрсетіңіз",
        splitExcess = "Төлемдер сомасы чек қорытындысынан асып тұр",
        rejectedKept = "Терілгені экранда қалды: түзетіп, қайта өткізіңіз"
    ),
    blockReason = BlockReasonTexts(
        unknown = "Касса бұғатталған",
        invalidToken = "Касса токені жарамсыз: кабинеттен жаңасын алып, баптауларға жазыңыз",
        shiftTooLong = "Ауысым тәуліктен ұзақ ашық: оны Z-есеппен жабыңыз",
        autonomousTooLong = "Автономды жұмыс рұқсат етілгеннен ұзақ жүрді: БФД байланысын қалпына келтіріңіз",
        deregistered = "Касса есептен шығарылды",
        disconnected = "Касса БФД-дан ажыратылған",
        incorrectData = "БФД касса деректерін қабылдамады",
        readingStays = "Бұғатталған кассаға оқу қалады: журнал, ауысымдар және есептер."
    ),
    units = UnitTexts(
        mapOf(
            "796" to "дана",
            "116" to "кг",
            "5114" to "қызм",
            "006" to "м",
            "112" to "л",
            "021" to "қума м",
            "168" to "т",
            "356" to "сағ",
            "359" to "тәул",
            "360" to "апта",
            "362" to "ай",
            "003" to "мм",
            "004" to "см",
            "005" to "дм",
            "642" to "бірл",
            "008" to "км",
            "160" to "гг",
            "161" to "мг",
            "162" to "кар",
            "163" to "г",
            "164" to "мкг",
            "110" to "мм³",
            "111" to "мл",
            "055" to "м²",
            "059" to "га",
            "061" to "км²",
            "625" to "парақ",
            "728" to "бума",
            "736" to "орам",
            "778" to "орама",
            "868" to "бөтелке",
            "931" to "жұмыс",
            "113" to "м³"
        )
    ),
    contact = buyerContactTextsKk,
    checkout = checkoutTextsKk,
    refusal = kassaRefusalTextsKk
)
