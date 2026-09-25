package kz.mybrain.superkassa.strings.impl.cabinet.register

import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterStateTexts

/** Надписи [RegisterStateTexts] по-русски. */
internal val registerStateTextsRu = RegisterStateTexts(
    disagree = "Показания расходятся",
    disagreeNote = "Расходятся: %s. Скорее всего, где-то состояние устарело — перечитайте карточку кассы; " +
        "если расхождение осталось, придётся разобраться, кто прав",
    sourceNode = "Эта машина",
    sourceCabinet = "Кабинет",
    sourceBfd = "БФД",
    working = "Касса в работе",
    blocked = "Касса заблокирована",
    offRecord = "Касса не на учёте",
    workUnknown = "О работе кассы не сказал никто",
    shiftUnknown = "Открыта ли смена, не знает никто",
    claimWorking = "касса в работе",
    claimBlocked = "касса заблокирована",
    claimOnRecord = "касса на учёте",
    claimOffRecord = "касса снята с учёта",
    claimRecordUnread = "учётная запись не прочитана",
    claimNotFiled = "на учёт не подавалась",
    claimIsnaPending = "заявление рассматривает КГД",
    claimIsnaRefused = "КГД отказал в учёте",
    claimNodeNoKkm = "этой кассы не знает",
    claimBfdNoKkm = "кассу ещё не видел",
    claimBfdNoAnswer = "о кассе не ответил",
    claimShiftOpen = "смена открыта",
    claimShiftClosed = "смена закрыта",
    claimShiftNotKept = "смену не ведёт"
)
