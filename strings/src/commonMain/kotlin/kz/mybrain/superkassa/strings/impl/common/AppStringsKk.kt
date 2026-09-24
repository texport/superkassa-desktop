package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.common.CommonStrings
import kz.mybrain.superkassa.strings.api.common.LoginStrings
import kz.mybrain.superkassa.strings.api.common.SectionStrings
import kz.mybrain.superkassa.strings.api.common.ShellStrings

/** Надписи [AppStrings] по-казахски. */
internal val appStringsKk = AppStrings(
    common = CommonStrings(
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
    login = LoginStrings(
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
    shell = ShellStrings(
        noKkm = "Касса таңдалмады",
        autonomous = "Дербес режим",
        blocked = "Бұғатталған",
        changeCashier = "Кассирді ауыстыру",
        moreActions = "Тағы"
    ),
    sections = SectionStrings(
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
    dashboard = dashboardStringsKk,
    autonomous = autonomousStringsKk,
    sale = saleStringsKk,
    returns = returnStringsKk,
    cash = cashStringsKk,
    queue = queueStringsKk,
    users = userStringsKk,
    settings = settingStringsKk,
    preview = previewStringsKk,
    status = statusStringsKk,
    enums = enumStringsKk
)
