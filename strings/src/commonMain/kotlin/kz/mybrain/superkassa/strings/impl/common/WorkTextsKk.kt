package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.AutonomousTexts
import kz.mybrain.superkassa.strings.api.common.CashTexts
import kz.mybrain.superkassa.strings.api.common.DashboardTexts
import kz.mybrain.superkassa.strings.api.common.ReceiptTexts
import kz.mybrain.superkassa.strings.api.common.ReturnTexts

/** Надписи [DashboardTexts] по-казахски. */
internal val dashboardTextsKk = DashboardTexts(
    state = "Күйі",
    shift = "Ауысым",
    shiftOpenNo = "№ %s ашық",
    shiftClosed = "Жабық",
    documentsInShift = "Ауысымдағы құжаттар",
    shiftDocuments = "Ауысым құжаттары",
    refused = "БФД қабылдамады",
    refusedHint = "Бұл құжаттарды БФД қабылдамады: олар фискалдық болмады, ауысым есептеуішіне кірмеді " +
        "және баспа түрі жоқ. Бас тарту коды мен кассир қызмет көрсетуге қажет.",
    openShift = "Ауысымды ашу",
    xReport = "X-есеп",
    closeShift = "Ауысымды жабу",
    closeShiftAsk = "Ауысымды жабып, Z-есеп алу керек пе?",
    closeShiftExplain = "Ауысымдағы құжат: %s, жәшікте %s. Z-есеп БФД-ға кетеді, ауысым қайтарылмайды.",
    closeShiftCashout = "Қалған қолма-қол ақша есеппен бірге алынады.",
    closeShiftKeepsCash = "Қалған қолма-қол ақша жәшікте қалып, жаңа ауысымға көшеді.",
    shiftTooLong = "Ауысым тәуліктен ұзақ ашық — оны Z-есеппен жабыңыз, әйтпесе БФД кассаны бұғаттайды.",
    shiftDayLimit = "Ауысымды %s дейін жабыңыз: одан кейін касса чекке, қайтаруға және ақшаға бас тартады.",
    openShiftHint = "Ауысым жабық — чек басу үшін ауысымды ашыңыз",
    openShiftAdmin = "Ауысымды әкімші ашады — күнді бастау үшін оны шақырыңыз.",
    shiftOpened = "Ауысым ашылды",
    shiftClosedDone = "Ауысым жабылды, Z-есеп жіберілді",
    xReportDone = "X-есеп жасалды",
    printForm = "Басып шығару пішіні",
    shiftUnknown = "Белгісіз",
    shiftEmpty = "Әзірге құжат жоқ",
    shiftEmptyHint = "Ауысым ашық: бірінші чек өткізілген бойда осында шығады.",
    documentsUnread = "Ауысым құжаттарын оқу мүмкін болмады",
    documentsUnreadHint = "Ауысым ашық, бірақ касса оның құжаттарын берген жоқ: саны белгісіз.",
    shiftUnknownHint = "Касса ауысым күйін айтпады — кассаны қайта оқыңыз; " +
        "көмектеспесе, қызмет көрсетушіні шақырыңыз.",
    documentNo = "№"

)

/** Надписи [AutonomousTexts] по-казахски. */
internal val autonomousTextsKk = AutonomousTexts(
    title = "Дербес режим",
    explain = "БФД-мен байланыс жоқ. Чектер басылып, дербес белгі алады, " +
        "ал байланыс пайда болған бойда БФД-ға өздері жіберіледі. Жұмысты жалғастыруға болады.",
    waiting = "Жіберуді күтуде",
    checkLink = "Байланысты тексеру",
    sendQueued = "Жиналғанды жіберу",
    sendQueuedRules = "Жинақталғанды әкімші жібереді: ауысым жабық болуы және касса бағдарламалау режимінде болуы " +
        "керек.",
    linkBack = "БФД-мен байланыс қалпына келді"
)

/** Надписи [ReceiptTexts] по-казахски. */
internal val receiptTextsKk = ReceiptTexts(
    receipt = "Чек",
    sale = "Сату",
    purchase = "Сатып алу",
    issueSale = "Чекті басу",
    issuePurchase = "Сатып алуды ресімдеу",
    issuing = "Ресімделуде…",
    name = "Атауы",
    price = "Бағасы",
    quantity = "Саны",
    measureUnit = "Өлшем бірлігі",
    vat = "ҚҚС",
    discount = "Жеңілдік",
    storno = "Сторно",
    stornoUndo = "Сторноны алып тастау",
    add = "Қосу",
    remove = "Алып тастау",
    clearBasket = "Себетті тазалау",
    receiptDiscount = "Чекке жеңілдік",
    receiptMarkup = "Чекке үстеме",
    discountOrMarkup = "Чекке жеңілдік пен үстеме — біреуі ғана",
    customerBin = "Сатып алушының ЖСН/БСН",
    taken = "Қабылданды",
    total = "Барлығы",
    barcode = "Штрих-код",
    barcodeSearch = "Штрих-код бойынша іздеу",
    barcodeSearching = "Ізделуде…",
    barcodeFind = "Табу",
    barcodeMissing = "Анықтамалықта мұндай штрих-код жоқ — позицияны қолмен қосыңыз.",
    barcodeUnavailable = "Анықтамалық қазір қолжетімсіз — позицияны қолмен қосыңыз.",
    payment = "Төлем түрі",
    delivered = "чек басылды және БФД-ға жеткізілді",
    queued = "чек басылды, байланыс жоқ — кезекке қойылды"
)

/** Надписи [ReturnTexts] по-казахски. */
internal val returnTextsKk = ReturnTexts(
    title = "Қайтару",
    empty = "Ағымдағы ауысымда қайтаруға негіз болатын чек жоқ.",
    receiptNo = "Чек №",
    refundFor = "№ чек бойынша қайтару",
    saleReturn = "Сатуды қайтару",
    giveBack = "Сатып алушыға қайтару",
    purchaseReturn = "Сатып алуды қайтару",
    takeBack = "Кері қабылдау",
    done = "ресімделді, күйі"
)

/** Надписи [CashTexts] по-казахски. */
internal val cashTextsKk = CashTexts(
    deposit = "Салу",
    withdraw = "Алу",
    deposited = "Салынды",
    withdrawn = "Алынды"
)
