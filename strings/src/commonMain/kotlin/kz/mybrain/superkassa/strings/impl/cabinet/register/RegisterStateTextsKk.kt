package kz.mybrain.superkassa.strings.impl.cabinet.register

import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterStateTexts

/** Надписи [RegisterStateTexts] по-казахски. */
internal val registerStateTextsKk = RegisterStateTexts(
    disagree = "Мәліметтер сәйкес емес",
    disagreeNote = "Сәйкес келмейді: %s. Бәлкім, бір жерде күй ескірген — касса карточкасын қайта оқыңыз; " +
        "сәйкессіздік қалса, кімнің дұрыс екенін анықтау керек",
    sourceNode = "Осы машина",
    sourceCabinet = "Кабинет",
    sourceBfd = "БФД",
    working = "Касса жұмыста",
    blocked = "Касса блокталған",
    offRecord = "Касса есепте тұрмайды",
    workUnknown = "Кассаның жұмысы туралы ешкім айтпады",
    shiftUnknown = "Ауысымның ашық-жабықтығын ешкім білмейді",
    claimWorking = "касса жұмыста",
    claimBlocked = "касса блокталған",
    claimOnRecord = "касса есепте тұр",
    claimOffRecord = "касса есептен шығарылған",
    claimRecordUnread = "есеп жазбасы оқылмаған",
    claimNotFiled = "есепке қоюға берілмеген",
    claimIsnaPending = "өтінішті МКК қарауда",
    claimIsnaRefused = "МКК есепке қоюдан бас тартты",
    claimNodeNoKkm = "бұл кассаны білмейді",
    claimBfdNoKkm = "кассаны әлі көрген жоқ",
    claimBfdNoAnswer = "касса туралы жауап бермеді",
    claimShiftOpen = "ауысым ашық",
    claimShiftClosed = "ауысым жабық",
    claimShiftNotKept = "ауысымды жүргізбейді"
)
