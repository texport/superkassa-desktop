package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.common.GeneralTexts
import kz.mybrain.superkassa.strings.api.common.LoginTexts
import kz.mybrain.superkassa.strings.api.common.SectionTexts
import kz.mybrain.superkassa.strings.api.common.TopBarTexts

/** Надписи [CommonTexts] по-казахски. */
internal val commonTextsKk = CommonTexts(
    general = GeneralTexts(
        refresh = "Жаңарту",
        hide = "Жасыру",
        print = "Басып шығару",
        amount = "Сома",
        pin = "ПИН",
        loading = "Оқылуда…",
        starting = "Касса іске қосылуда",
        noAnswer = "Жауап келмеді — құжат өтіп кетуі мүмкін, журналды тексеріңіз; қайталау қосарламайды",
        kassaFailed = "Касса әрекетті орындамады — қайталаңыз; болмаса, қызмет көрсетушіні шақырыңыз",
        refusalCode = "Бас тарту коды",
        deliveredToOfd = "БФД-ға жеткізілді",
        queuedNoLink = "байланыс жоқ — кезекке қойылды",
        deliveryState = "Жеткізу күйі",
        collapse = "Жию",
        explain = "Түсіндірме",
        expand = "Жаю",
        retry = "Қайталау",
        nothingToPick = "Таңдайтын ештеңе жоқ"
    ),
    login = LoginTexts(
        title = "Кассаға кіру",
        search = "Іздеу: нөмір, атауы, ұйым",
        noKkmsTitle = "Бұл жұмыс орнында бірде-бір касса жоқ",
        noKkms = "Мұнда әлі бірде-бір касса тіркелмеген. Касса тіркеңіз немесе кабинеттен бастаңыз.",
        kkmsUnreadTitle = "Кассалар тізімі оқылмады",
        kkmsUnread = "Касса кассалар тізімін бермеді, мұнда қанша касса бары белгісіз. " +
            "Қайталаңыз; болмаса, қызмет көрсетушіні шақырыңыз.",
        yourKkm = "Сіздің кассаңыз",
        pick = "Таңдау",
        picked = "Таңдалды",
        enter = "Кіру",
        reload = "Тізімді жаңарту",
        noKkmChosen = "Касса таңдалмады",
        pickHint = "Тізімнен кассаны таңдаңыз",
        factory = "Зауыттық",
        registrationNumber = "Тіркеу нөмірі"
    ),
    topBar = TopBarTexts(
        noKkm = "Касса таңдалмады",
        autonomous = "Дербес режим",
        blocked = "Бұғатталған",
        changeCashier = "Кассирді ауыстыру",
        moreActions = "Тағы"
    ),
    sections = SectionTexts(
        dashboard = "Басты бет",
        sale = "Сату",
        returns = "Қайтару",
        cash = "Ақша",
        history = "Тарих",
        queue = "Кезек",
        users = "Кассирлер",
        settings = "Баптаулар",
        register = "Жаңа касса",
        cabinet = "БФД кабинеті"
    ),
    dashboard = dashboardTextsKk,
    autonomous = autonomousTextsKk,
    receipt = receiptTextsKk,
    returns = returnTextsKk,
    cash = cashTextsKk,
    queue = queueTextsKk,
    users = userTextsKk,
    settingsScreen = settingsScreenTextsKk,
    preview = previewTextsKk,
    status = statusTextsKk,
    enums = enumTextsKk
)
