package kz.mybrain.superkassa.strings.impl.kassa.refusal

import kz.mybrain.superkassa.strings.api.kassa.refusal.KassaRefusalTexts

/** Надписи [KassaRefusalTexts] по-казахски. */
internal val kassaRefusalTextsKk = KassaRefusalTexts(
    programming = "Касса бағдарламалау режимінде: касса баптауларында одан шығыңыз",
    vatNotPayer = "Касса ҚҚС төлеушісі емес: позицияларға «ҚҚС-сыз» қойыңыз",
    vatUnknown = "Касса мұндай ҚҚС мөлшерлемесін білмейді: тізімнен мөлшерлеме таңдаңыз",
    paymentUnsupported = "Касса бұл төлем түрін қазір қабылдамайды: басқасын таңдаңыз",
    unitUnknown = "Касса мұндай өлшем бірлігін білмейді: тізімнен бірлік таңдаңыз",
    outOfRange = "Баға, саны немесе жеңілдік рұқсат етілгеннен тыс: позициялар мен төлемді тексеріңіз",
    basisRequired = "Қайтару үшін негіз чекті таңдаңыз",
    unknown = "Касса бас тартты, бас тарту коды %s"
)
