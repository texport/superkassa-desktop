package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.common.GeneralTexts
import kz.mybrain.superkassa.strings.api.common.LoginTexts
import kz.mybrain.superkassa.strings.api.common.SectionTexts
import kz.mybrain.superkassa.strings.api.common.StatusHints
import kz.mybrain.superkassa.strings.api.common.TopBarTexts
import kz.mybrain.superkassa.strings.impl.share.shareTextsKk

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
        notAccepted = "%1\$s, бірақ БФД оны қабылдамады: %2\$s",
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
        statusHints = StatusHints(
            active = "Касса жұмыста: чек басып, оларды БФД-ға жібереді.",
            blocked = "Касса бұғатталған: БФД оның чектерін қабылдамайды. Себебін және бұғатты алуды " +
                "БФД кабинетінен қараңыз.",
            programming = "Касса бағдарламалау режимінде: ол қосулы тұрғанда чек басылмайды. Өшіру — касса " +
                "баптауларында, «Негізгі» бөлімінде.",
            registration = "Касса МКК-да әлі есепке қойылмаған: чек басуға болмайды. БФД кабинетінде өтініш беріңіз.",
            autonomous = "БФД-мен байланыс жоқ: чектер басылып, кезекте жиналады, байланыс қалпына " +
                "келгенде касса оларды өзі жібереді.",
            shiftOpen = "Ауысым ашық: чек басуға болады. Тәулік біткенше оны басты беттегі Z-есеппен жабыңыз.",
            shiftClosed = "Ауысым жабық: чек басу үшін басты бетте ауысымды ашыңыз."
        ),
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
        cabinet = "БФД кабинеті",
        kkms = "Кассалар",
        menu = "Бөлімдер"
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
    share = shareTextsKk,
    status = statusTextsKk,
    enums = enumTextsKk
)
