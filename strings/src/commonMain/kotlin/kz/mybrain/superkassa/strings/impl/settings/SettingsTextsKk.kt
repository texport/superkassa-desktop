package kz.mybrain.superkassa.strings.impl.settings

import kz.mybrain.superkassa.strings.api.settings.CoreSettingTexts
import kz.mybrain.superkassa.strings.api.settings.DeliveryFieldTexts
import kz.mybrain.superkassa.strings.api.settings.DeliverySettingTexts
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
    delivery = DeliverySettingTexts(
        title = "Чекті сатып алушыға жеткізу",
        hint = "Қосылған арна чекті сатып алушыға қызмет арқылы жібереді: SMS шлюзі, Telegram боты, WhatsApp " +
            "немесе пошта сервері. Жұмыс орнының барлық кассасына ортақ. Мекенжайлар мен кілттер келесі чектен, " +
            "арнаны қосу касса қайта іске қосылғаннан кейін әрекет етеді.",
        recipient = "Чек сатып алушының чекте көрсетілген байланысына жіберіледі: телефон — SMS және " +
            "WhatsApp арқылы, пошта — хатпен, чат — Telegram-ға. Байланыс болмаса, чек сатып алушыға жіберілмейді.",
        configured = "Бапталған",
        notConfigured = "Бапталмаған",
        secretHint = "Берілген кілттер *** белгісімен жасырылған: кілтті алып тастау үшін өшіріңіз " +
            "немесе жаңасын теріңіз.",
        malformed = "Жазылуын тексеріңіз",
        portRange = "1-ден 65535-ке дейінгі бүтін сан",
        saved = "Чекті жеткізу сақталды. Арнаны қосу касса қайта іске қосылғаннан кейін әрекет етеді",
        channels = deliveryChannels("Пошта"),
        fields = DeliveryFieldTexts(
            smsUrl = "{phone} және {text} бар шлюз мекенжайы",
            smsKey = "SMS шлюзінің кілті",
            telegramToken = "Бот токені",
            whatsAppToken = "Қол жеткізу кілті",
            whatsAppSender = "Жіберуші нөмірі (ID)",
            emailHost = "Пошта сервері",
            emailPort = "Порт",
            emailUser = "Пайдаланушы",
            emailPassword = "Құпиясөз",
            emailFrom = "Жіберуші мекенжайы"
        )
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
