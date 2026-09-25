package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.EnumTexts
import kz.mybrain.superkassa.strings.api.common.PreviewTexts
import kz.mybrain.superkassa.strings.api.common.StatusTexts

/** Надписи [PreviewTexts] по-казахски. */
internal val previewTextsKk = PreviewTexts(
    title = "Басып шығару пішіні",
    narrower = "Тарырақ",
    wider = "Кеңірек",
    close = "Жабу",
    missing = "Касса басып шығару пішінін салмады",
    print = "Басып шығару",
    printSent = "Принтерге жіберілді",
    printFailed = "Принтер тапсырманы қабылдамады",
    printerMissing = "Бұл машинада бірде-бір принтер жоқ: басып шығаратын орын жоқ",
    save = "Файлға сақтау",
    saved = "Сақталды",
    zoomIn = "Үлкейту",
    zoomOut = "Кішірейту",
    fit = "Ені бойынша",
    drawPin = "Касса пині",
    drawPinHint = "Басып шығару пішінін касса салады және оған пін бойынша жібереді. " +
        "Пін тек қолданба жадында қалады және касса бөлімдерін ашпайды.",
    draw = "Пішінді көрсету",
    noDrawer = "Құжат кабинеттен келді, ал ол бойынша басып шығару пішінін осы машинадағы " +
        "касса салады. Мұнда бірде-бір касса тіркелмеген — салатын ештеңе жоқ."
)

/** Надписи [StatusTexts] по-казахски. */
internal val statusTextsKk = StatusTexts(
    delivered = "Жеткізілді",
    resent = "Кейін жіберілген",
    refused = "Қабылданбады",
    internal = "Ішкі",
    queued = "Кезекте",
    unknown = "Күйі белгісіз"
)

/** Надписи [EnumTexts] по-казахски. */
internal val enumTextsKk = EnumTexts(
    vatNone = "ҚҚС-сыз",
    vat0 = "ҚҚС 0%",
    vat5 = "ҚҚС 5%",
    vat10 = "ҚҚС 10%",
    vat16 = "ҚҚС 16%",
    paymentCash = "Қолма-қол",
    paymentCard = "Карта",
    paymentElectronic = "Электрондық",
    paymentMobile = "Мобильді төлем",
    paymentCredit = "Несиеге",
    paymentTare = "Ыдыспен",
    domainTrading = "Сауда",
    domainServices = "Қызметтер",
    domainHotels = "Қонақүйлер",
    domainGasOil = "Мұнай өнімдері",
    domainTaxi = "Такси",
    domainParking = "Тұрақ",
    docCheck = "Чек",
    docSale = "Сату",
    docReturn = "Сатуды қайтару",
    docBuy = "Сатып алу",
    docBuyReturn = "Сатып алуды қайтару",
    docShiftOpen = "Ауысымды ашу",
    docShiftClose = "Ауысымды жабу",
    docCashIn = "Салу",
    docCashOut = "Алу",
    docReportX = "X-есеп",
    stateActive = "Жұмыста",
    stateIdle = "Жұмысқа дайын",
    stateBlocked = "Бұғатталған",
    stateProgramming = "Бағдарламалау",
    stateRegistration = "Тіркеу"
)
