package kz.mybrain.superkassa.strings.impl.cabinet.refusal

import kz.mybrain.superkassa.strings.api.cabinet.refusal.CabinetRefusalTexts

/** Надписи [CabinetRefusalTexts] по-казахски. */
internal val cabinetRefusalTextsKk = CabinetRefusalTexts(
    unreachable = "Кабинет көрсетілген мекенжайда жауап бермейді",
    unreadable = "Кабинет қолданба оқи алмайтын жауап қайтарды",
    sessionExpired = "Рұқсат мерзімі бітті — қайта кіріңіз",
    noNcaLayer = "NCALayer жауап бермейді. Оны іске қосып, қайта кіріңіз",
    signDeclined = "Қолтаңба алынбады",
    signWindowClosed = "Қол қою терезесі жабылды",
    signCancelled = "Қол қою NCALayer-де тоқтатылды",
    kkmNotActive = "Касса деректерді қабылдау серверінде қызмет көрсетуге жіберілмеген",
    defaultPinNotAllowed = "Кассада әдепкі пин тұр — оны кассаның өзінде ауыстырыңыз",
    accessDenied = "БФД кабинеті бұл компания үшін жабық — қолжетімділікті ашу үшін БФД-ға хабарласыңыз"
)
