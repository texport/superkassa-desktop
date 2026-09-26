package kz.mybrain.superkassa.strings.impl.kassa

import kz.mybrain.superkassa.strings.api.kassa.BlockReasonTexts
import kz.mybrain.superkassa.strings.api.kassa.KassaTexts
import kz.mybrain.superkassa.strings.api.kassa.PaymentTexts
import kz.mybrain.superkassa.strings.api.kassa.UnitTexts
import kz.mybrain.superkassa.strings.impl.kassa.checkout.checkoutTextsRu
import kz.mybrain.superkassa.strings.impl.kassa.contact.buyerContactTextsRu
import kz.mybrain.superkassa.strings.impl.kassa.refusal.kassaRefusalTextsRu

/** Надписи [KassaTexts] по-русски. */
internal val kassaTextsRu = KassaTexts(
    money = moneyTextsRu,
    sale = saleTextsRu,
    payment = PaymentTexts(
        addPayment = "Добавить оплату",
        removePayment = "Убрать оплату",
        amount = "Сумма",
        rest = "Остаток итога — касса считает сама",
        splitEmpty = "Укажите сумму каждой оплаты, кроме последней",
        splitExcess = "Суммы оплат больше итога чека",
        rejectedKept = "Набранное осталось на экране: исправьте и проведите снова"
    ),
    blockReason = BlockReasonTexts(
        unknown = "Касса заблокирована",
        invalidToken = "Токен кассы недействителен: получите новый в кабинете и впишите его в настройках",
        shiftTooLong = "Смена открыта дольше суток: закройте её Z-отчётом",
        autonomousTooLong = "Автономная работа шла дольше допустимого: восстановите связь с БФД",
        deregistered = "Касса снята с учёта",
        disconnected = "Касса отключена от БФД",
        incorrectData = "БФД отверг данные кассы",
        readingStays = "Заблокированной кассе остаётся чтение: журнал, смены и отчёты."
    ),
    units = UnitTexts(
        mapOf(
            "796" to "шт",
            "116" to "кг",
            "5114" to "усл",
            "006" to "м",
            "112" to "л",
            "021" to "пог. м",
            "168" to "т",
            "356" to "ч",
            "359" to "сут",
            "360" to "нед",
            "362" to "мес",
            "003" to "мм",
            "004" to "см",
            "005" to "дм",
            "642" to "ед",
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
            "625" to "лист",
            "728" to "пач",
            "736" to "рул",
            "778" to "упак",
            "868" to "бут",
            "931" to "раб",
            "113" to "м³"
        )
    ),
    contact = buyerContactTextsRu,
    checkout = checkoutTextsRu,
    refusal = kassaRefusalTextsRu
)
