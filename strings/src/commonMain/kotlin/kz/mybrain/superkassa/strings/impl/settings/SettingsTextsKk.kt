package kz.mybrain.superkassa.strings.impl.settings

import kz.mybrain.superkassa.strings.api.settings.CoreSettingTexts
import kz.mybrain.superkassa.strings.api.settings.KassaFactsTexts
import kz.mybrain.superkassa.strings.api.settings.LookTexts
import kz.mybrain.superkassa.strings.api.settings.SettingsTexts

/** Надписи [SettingsTexts] по-казахски. */
internal val settingsTextsKk = SettingsTexts(
    core = CoreSettingTexts(
        title = "БФД-мен алмасу мерзімдері",
        hint = "Жұмыс орнының барлық кассасына ортақ: БФД жауабын қанша күту және байланысты қашан қайта тексеру. " +
            "Жаңа мәндер касса қайта іске қосылғаннан кейін күшіне енеді.",
        unread = "Касса өз баптауларын бермеді",
        mode = "Жұмыс режимі",
        modeDesktop = "Жұмыс орны",
        modeServer = "Сервер",
        reconnect = "БФД байланысын қайталау, с",
        seconds = "Нөлден үлкен бүтін секунд саны",
        protocolFixed = "Хаттама нұсқасын кассаның іске қосылуы береді, мұнда ол өзгермейді.",
        frozen = "Өзгерту жабық",
        frozenHint = "Касса баптауларын өзгертуге жұмыс орнының иесі оның баптаулар файлында тыйым салған: мұнда " +
            "оларды тек көруге болады.",
        serverHint = "Касса сервер ретінде жұмыс істейді: оның баптаулары жұмыс орнында емес, серверде өзгертіледі.",
        saved = "Касса баптаулары сақталды. Олар касса қайта іске қосылғаннан кейін күшіне енеді",
        autoClose = "Бір тәуліктен кейін ауысымды өзі жабу",
        autoCloseHint = "Ауысым бір тәуліктен ұзақ бола алмайды: касса чек ресімдеуді тоқтатады. Бұл ауыстырғышпен " +
            "кассир үлгермесе, касса ауысымды өзі жауып, Z-есепті алады."
    ),
    facts = KassaFactsTexts(
        title = "Бағдарлама және касса ядросы",
        hint = "Қолдау қызметі алдымен сұрайтыны: қандай нұсқалар тұр, касса қалай жұмыс істейді және деректері " +
            "қайда жатыр. Кіргенге дейін де көрінеді — касса ашылмағанда немесе кіргізбегенде.",
        appVersion = "Қолданба нұсқасы",
        coreVersion = "Ядро нұсқасы",
        dataDirectory = "Деректер каталогы",
        kkmCount = "Осы машинадағы кассалар",
        unread = "Касса жауап бермеді",
        ofdAuth = "БФД авторизация деректері",
        nextRequest = "Келесі сұраныс нөмірі"
    ),
    sections = settingsSectionsKk
)

/** Надписи [LookTexts] по-казахски. */
internal val lookTextsKk = LookTexts(
    theme = "Тақырып",
    accent = "Реңк",
    accentHint = "Түймелердің, ерекшелеудің және белгішелердің негізгі түсі. " +
        "Бас тарту кез келген реңкте қызыл болып қалады.",
    accentRed = "Қызыл",
    accentOrange = "Қызғылт сары",
    accentAmber = "Кәріптас",
    accentOlive = "Зәйтүн",
    accentLime = "Лайм",
    accentGreen = "Жасыл",
    accentEmerald = "Зүмірет",
    accentTeal = "Көгілдір",
    accentAzure = "Ашық көк",
    accentBlue = "Көк",
    accentIndigo = "Индиго",
    accentViolet = "Күлгін",
    accentLilac = "Сирень",
    accentPink = "Қызғылт",
    typeface = "Қаріп",
    typefaceSystem = "Жүйелік",
    typefaceSans = "Кертіксіз",
    typefaceSerif = "Кертікті",
    typefaceMono = "Бір енді",
    textScale = "Өлшем",
    textScaleHint = "Тығыз — тауар тізіміне, ірілері — касса үстеліне: олармен қорытынды бір метрден оқылады.",
    textScaleDense = "Тығыз",
    textScaleCompact = "Ықшам",
    textScaleNormal = "Қалыпты",
    textScaleLarge = "Үлкен",
    textScaleLarger = "Ірірек"
)
