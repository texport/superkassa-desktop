package kz.mybrain.superkassa.strings.impl.cabinet.register

import kz.mybrain.superkassa.strings.api.cabinet.register.RegisterTexts

/** Надписи [RegisterTexts] по-русски. */
internal val registerTextsRu = RegisterTexts(
    passport = "Паспорт кассы",
    internalName = "Название кассы",
    factoryNumber = "Заводской номер",
    factoryLocked = "Записан в регистрационной карте — меняется только перерегистрацией",
    manufactureYear = "Год выпуска",
    model = "Модель",
    registrationNumber = "Регистрационный номер",
    status = "Состояние",
    technicalState = "Техническое состояние",
    technicalUnknown = "БФД эту кассу ещё не видел",
    trafficSuspended = "Приём документов приостановлен",
    bfdDisconnected = "Связь с БФД разорвана",
    shift = "Смена",
    lastContact = "Последняя связь",
    lastContactNever = "касса ещё не выходила на связь",
    shiftNumberTitle = "Номер смены",
    shiftNumberNone = "смен ещё не было",
    token = "Токен",
    issueToken = "Выдать токен",
    tokenIssued = "Токен выдан",
    tokenGoesToNode = "Новый токен вписан в кассу на этой машине",
    tokenUnconfirmed = "Сервис приёма БФД ещё не подтвердил новый токен — " +
        "повторите выдачу через минуту",
    tokenNeedsNode = "Касса работает на этой машине: впишите новый токен в её настройках, иначе она встанет на " +
        "первом же чеке",
    tokenOnlyRegistered = "Токен выдаётся кассе, стоящей на учёте, и только когда по ней нет поданного заявления",
    localInfoSynced = "Сведения кассы на этой машине обновлены",
    localInfoNeedsSync = "Новая точка появится в чеках после сверки сведений в настройках кассы: сейчас её не сделать",
    delete = "Удалить кассу",
    deleteOnlyDraft = "Удалить можно только кассу, ещё не поставленную на учёт",
    actionsJournal = "Журнал действий",
    actionsEmpty = "Регистрационных действий не было",
    state = registerStateTextsRu,
    card = registrationCardTextsRu
)
