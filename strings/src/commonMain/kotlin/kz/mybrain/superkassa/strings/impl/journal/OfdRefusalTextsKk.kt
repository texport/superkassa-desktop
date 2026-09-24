package kz.mybrain.superkassa.strings.impl.journal

import kz.mybrain.superkassa.strings.api.journal.OfdRefusalTexts

/** Надписи [OfdRefusalTexts] по-казахски. */
internal val ofdRefusalTextsKk = OfdRefusalTexts(
    unknownId = "БФД бұл кассаны білмейді",
    invalidToken = "Касса токені жарамсыз: кабинеттен жаңасын алыңыз",
    protocolError = "БФД касса сұранысын талдамады",
    unknownCommand = "БФД мұндай пәрменді білмейді",
    unsupportedCommand = "БФД мұндай пәрменді қолдамайды",
    invalidConfiguration = "БФД касса баптауларын дұрыс емес деп санайды",
    invalidRequestNumber = "БФД күтпеген нөмірмен сұраныс алды",
    invalidRetry = "БФД сұраныс қайталауын қабылдамады",
    openShiftTimeout = "Ауысым тәуліктен ұзақ ашық: оны жабыңыз",
    incorrectData = "БФД чек деректерін қабылдамады",
    notEnoughCash = "БФД деректері бойынша ақша жәшігінде қолма-қол ақша жеткіліксіз",
    blocked = "БФД кассаны бұғаттады",
    sameTaxpayer = "Сатып алушы мен сатушы — бір тұлға",
    deregistered = "Касса есептен шығарылды",
    disconnected = "Касса БФД-дан ажыратылған",
    serviceUnavailable = "БФД уақытша қолжетімсіз",
    unknownError = "БФД себебін айтпай бас тартты"
)
