package kz.mybrain.superkassa.strings.impl.cabinet.register

import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterTexts

/** Надписи [RegisterTexts] по-казахски. */
internal val registerTextsKk = RegisterTexts(
    passport = "Касса төлқұжаты",
    internalName = "Касса атауы",
    factoryNumber = "Зауыттық нөмір",
    factoryLocked = "Тіркеу картасына жазылған — тек қайта тіркеу арқылы өзгереді",
    manufactureYear = "Шыққан жылы",
    model = "Үлгі",
    registrationNumber = "Тіркеу нөмірі",
    status = "Күйі",
    technicalState = "Техникалық күйі",
    technicalUnknown = "БФД бұл кассаны әлі көрген жоқ",
    trafficSuspended = "Құжаттарды қабылдау тоқтатылған",
    bfdDisconnected = "БФД-мен байланыс үзілген",
    shift = "Ауысым",
    lastContact = "Соңғы байланыс",
    lastContactNever = "касса әлі байланысқа шыққан жоқ",
    shiftNumberTitle = "Ауысым нөмірі",
    shiftNumberNone = "ауысым әлі болған жоқ",
    token = "Токен",
    issueToken = "Токен беру",
    tokenIssued = "Токен берілді",
    tokenGoesToNode = "Жаңа токен осы машинадағы кассаға жазылды",
    tokenUnconfirmed = "БФД қабылдау қызметі жаңа токенді әлі растамады — " +
        "бір минуттан кейін қайта беріңіз",
    tokenNeedsNode = "Касса осы машинада жұмыс істейді: жаңа токенді оның баптауларына жазыңыз, әйтпесе ол алғашқы " +
        "чекте тоқтайды",
    tokenOnlyRegistered = "Токен есепке қойылған кассаға және ол бойынша берілген өтініш болмағанда ғана беріледі",
    localInfoSynced = "Осы машинадағы касса мәліметтері жаңартылды",
    localInfoNeedsSync = "Жаңа нүкте чектерде касса баптауларындағы салыстырудан кейін шығады: қазір оны жасау " +
        "мүмкін емес",
    delete = "Кассаны жою",
    deleteOnlyDraft = "Тек есепке қойылмаған кассаны жоюға болады",
    actionsJournal = "Әрекеттер журналы",
    actionsEmpty = "Тіркеу әрекеттері болған жоқ",
    state = registerStateTextsKk,
    card = registrationCardTextsKk
)
