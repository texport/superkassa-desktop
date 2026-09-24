package kz.mybrain.superkassa.strings.impl.kassa.refusal

import kz.mybrain.superkassa.strings.api.kassa.refusal.KassaRefusalTexts

/** Надписи [KassaRefusalTexts] по-русски. */
internal val kassaRefusalTextsRu = KassaRefusalTexts(
    programming = "Касса в режиме программирования: выйдите из него в настройках кассы",
    vatNotPayer = "Касса не плательщик НДС: поставьте позициям «Без НДС»",
    vatUnknown = "Такую ставку НДС касса не знает: выберите ставку из списка",
    paymentUnsupported = "Этот вид оплаты касса сейчас не принимает: выберите другой",
    unitUnknown = "Такую единицу измерения касса не знает: выберите единицу из списка",
    outOfRange = "Цена, количество или скидка вне допустимого: проверьте позиции и оплату",
    basisRequired = "Для возврата выберите чек-основание",
    unknown = "Касса отказала, код отказа %s"
)
