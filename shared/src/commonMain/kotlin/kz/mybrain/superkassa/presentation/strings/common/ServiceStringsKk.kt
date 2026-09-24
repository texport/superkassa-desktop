package kz.mybrain.superkassa.presentation.strings.common

import kz.mybrain.superkassa.presentation.strings.settings.kazakhLook

/**
 * Надписи служебных разделов на этом языке.
 *
 * Настройки, показ печатной формы и словари состояний вынесены из общего
 * набора: одних настроек сто строк, и рядом с ними надписи продажи
 * не найти глазами.
 */

internal val kazakhSettings = SettingStrings(
    title = "Баптаулар",
    currentKkm = "Жұмыстағы касса",
    orgUnknown = "Ұйым көрсетілмеген",
    changeKkm = "Кассаны ауыстыру",
    registerKkm = "Касса тіркеу",
    back = "Артқа",
    kkmIdentifier = "БКМ идентификаторы",
    token = "Токен",
    adminPin = "Әкімші пині",
    registering = "Тіркелуде…",
    register = "Тіркеу",
    registered = "Касса тіркелді, күйі",
    diagnostics = "Диагностика",
    ofdLink = "БФД байланысы",
    checkOfdLink = "БФД байланысын тексеру",
    ofdAnswers = "БФД жауап береді",
    ofdSilent = "БФД жауап бермейді",
    ofdInfo = "БФД туралы мәліметтер",
    programmingMode = "Бағдарламалау режимі",
    enterProgramming = "Бағдарламалауға кіру",
    exitProgramming = "Бағдарламалаудан шығу",
    programmingOn = "Қосулы",
    enteredProgramming = "Касса бағдарламалау режиміне ауыстырылды",
    exitedProgramming = "Касса бағдарламалау режимінен шығарылды",
    ofd = "БФД",
    environment = "Контур",
    language = "Тіл",
    appearance = "Безендіру",
    appearanceHint = "Осы жұмыс орнының тақырыбы мен тілі. Осы машинада сақталады: чектерде, басқа кассаларда және кабинетте " +
        "ештеңе өзгермейді",
    look = kazakhLook,
    ofdToken = "БФД токені",
    newToken = "Жаңа токен",
    saveToken = "Токенді сақтау",
    tokenSaved = "Токен сақталды",
    tokenHint = "Токенді БФД береді. «Токен жарамсыз» жауабынан кейін касса тоқтайды " +
        "және тек жаңа токен енгізілгенде қайта жұмыс істейді.",
    printForm = "Басып шығару пішіні",
    printFormHint = "Басылатын чектің көрінісі: тілі, таспа ені және нені басу керек. Пішім БФД-ға кеткенмен сәйкес келуге тиіс — " +
        "бұл бір құжат",
    programmingRequired = "Касса баптаулары бағдарламалау режимінде өзгертіледі.",
    printFormSaved = "Чек баптаулары сақталды",
    receiptLanguage = "Чек тілі",
    receiptBoth = "Екі тіл",
    receiptKk = "Қазақша",
    receiptRu = "Русский",
    paperWidth = "Таспа ені",
    printLayout = "Басып шығару макеті",
    printer = "Касса принтері",
    printerHint = "Принтер кассаға бекітіледі: бір компьютерде екеуі болуы мүмкін, " +
        "әрқайсысының чек таспасы бөлек. Таңдалмаған кезде тапсырма жүйелік принтерге кетеді.",
    printerSystem = "Жүйелік әдепкі",
    printerNone = "Бұл машинада бірде-бір принтер жоқ: чекті басып шығаратын орын жоқ. " +
        "Принтерді қосып, баптауларды қайта ашыңыз",
    printKind = "Сақтау кезіндегі файл түрі",
    printCopies = "Басып шығару даналары",
    panelBehaviour = "Касса бағанының бөлімдері",
    taxSettings = "Касса салықтары",
    taxSettingsHint = "Осы кассаның салық режимі мен ҚҚС мөлшерлемесі: әр чектегі салық солардан есептеледі. МКК деректері бойынша " +
        "қойылады — сәйкессіздікте чектер дұрыс емес салықпен кетеді",
    taxRegime = "Салық режимі",
    tradeDomain = "Кассаның саласы",
    tradeDomainHint = "Хаттама әр чектен сала түрін талап етеді, ал кассаның саласы біреу: осында таңдалғаны " +
        "кассирдің сатуда қандай деректемелерді толтыратынын шешеді. Саудада олар жоқ",
    domainKind = "Сала түрі",
    domainFields = "Кассир сатуда толтырады",
    defaultVatGroup = "Әдепкі мөлшерлеме",
    dictionariesMissing = "Касса анықтамалықтары оқылмаған",
    dictionariesMissingHint = "Салық режимдері мен ҚҚС мөлшерлемелері кассадан келеді. Жұмыс орны оларды сұрағанда " +
        "касса жауап бермеді — қайталап көріңіз.",
    autoCashout = "Ауысым жабылғанда қолма-қол ақшаны алу",
    autoCashoutHint = "Касса Z-есеппен бірге қалған қолма-қол ақшаны алуды өзі ресімдейді. Онсыз жәшіктегі ақша жаңа " +
        "ауысымға көшеді де, кассадағы қалдық жәшіктегімен үйлеспейді.",
    settingsSaved = "Касса баптаулары сақталды",
    addressMalformed = "Мекенжай http:// немесе https:// деп басталады және бос орын болмайды",
    workplace = "Жұмыс орнының баптаулары",
    mapServices = "Карта қызметтері",
    mapServicesHint = "Мекенжай көрсетілмегенше, қоғамдастықтың ашық картасы жұмыс істейді: ол барлық иелерге есептелмеген",
    mapTiles = "Карта тақтайшалары",
    mapSearch = "Мекенжай іздеу",
    mapReverse = "Белгі бойынша мекенжай",
    mapLocation = "Орынды анықтау",
    mapDefault = "Ашық қызметтерге қайтару",
    householdWorkplace = "Қолданба",
    householdKkm = "Касса",
    householdCabinet = "БФД кабинеті",
    groupExchange = "БФД-мен алмасу",
    groupPrinting = "Басып шығару",
    groupService = "Касса баптаулары",
    groupServices = "Қызметтер мекенжайлары",
    groupProgram = "Осы машинадағы бағдарлама",
    groupIrreversible = "Қайтарымсыз",
    panelBehaviourHint = "Мұнда таңдалғанды кассир сатуды ашқанда көреді. " +
        "Бөлімді экранның өзінде де көрсеткішпен жинауға немесе жаюға болады.",
    panelPositionEntry = "Жаңа позиция",
    panelReceiptChanges = "Жеңілдіктер мен үстемелер",
    panelCustomerData = "Сатып алушының деректері",
    panelMoney = "Төлем және қорытынды",
    printLayoutHint = "58 және 80 мм таспа — чек принтерлеріне, бет — кәдімгі параққа басып шығаруға " +
        "және сатып алушыға жіберуге.",
    layoutTape58 = "58 мм таспа",
    layoutTape80 = "80 мм таспа",
    layoutFullscreen = "Толық бет",
    printOfdAds = "БФД жарнамасын басып шығару",
    printOfdAdsHint = "Жолдар чекке жауаппен бірге келеді және қорытынды астында басылады.",
    receiptLines = "Чектегі өз жолдарыңыз",
    receiptLinesHint = "Оператордың жарнамасы БФД-дан келеді, ал бұл — сауда нүктесінің өз жолдары: " +
        "сәлемдесу, қайтару шарттары, алғыс. Бос өріс басылмайды.",
    saveReceiptLines = "Жолдарды сақтау",
    lineBeforeHeader = "Тақырып үстінде",
    lineHeader = "Тақырыпта",
    lineAfterHeader = "Тақырып астында",
    lineBeforeItems = "Позициялар алдында",
    lineAfterItems = "Позициялардан кейін",
    lineBeforeTotals = "Қорытынды алдында",
    lineAfterTotals = "Қорытындыдан кейін",
    lineBeforeQr = "QR-код алдында",
    lineFooter = "Төменгі жолда",
    appearanceSystem = "Жүйедегідей",
    appearanceLight = "Ашық",
    appearanceDark = "Күңгірт",
    factoryStep = "1-қадам. Зауыттық деректер",
    generateFactory = "Жасау",
    factoryNumber = "Зауыттық нөмір",
    manufactureYear = "Шығарылған жылы",
    ofdStep = "2-қадам. БФД деректері",
    localName = "Касса атауы",
    localNameHint = "Атау кассада сақталады және кіру экранында көрінеді. " +
        "Өзге мәліметтер БФД-дан келеді және мұнда өзгермейді.",
    save = "Сақтау"
)

internal val kazakhPreview = PreviewStrings(
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

internal val kazakhStatus = StatusStrings(
    delivered = "Жеткізілді",
    resent = "Кейін жіберілген",
    refused = "Қабылданбады",
    internal = "Ішкі",
    queued = "Кезекте",
    unknown = "Күйі белгісіз"
)

internal val kazakhEnums = EnumStrings(
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
