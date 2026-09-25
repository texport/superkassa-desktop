package kz.mybrain.superkassa.strings.impl.cabinet.applications

import kz.mybrain.superkassa.strings.api.cabinet.applications.ApplicationTexts

/** Надписи [ApplicationTexts] по-русски. */
internal val applicationTextsRu = ApplicationTexts(
    title = "Заявления в КГД",
    registration = "Поставить на учёт",
    reregistration = "Перерегистрировать",
    deregistration = "Снять с учёта",
    reason = "Причина",
    reasonCessation = "Не используется",
    reasonBroken = "Неисправна",
    reasonLost = "Утеряна",
    reasonOther = "Иное",
    comment = "Комментарий",
    newPlace = "Новая торговая точка",
    registerStatus = "Состояние кассы",
    submit = "Подать заявление",
    sent = "Заявление отправлено",
    wait = "Ответ КГД приходит не сразу — журнал ниже обновится сам",
    failed = "Заявление не отправлено",
    inFlight = "Заявление уже подано — новое можно подать после ответа КГД",
    none = "Касса снята с учёта — заявления по ней больше не подаются",
    stagePreparing = "Готовим заявление",
    stageSigning = "Ждём подпись в NCALayer",
    stageSending = "Отправляем в кабинет",
    shiftOpenTitle = "Смена не закрыта",
    shiftOpenAsk = "Снять кассу с учёта можно только при закрытой смене. Закрыть смену на этой кассе и подать " +
        "заявление?",
    shiftOpenElsewhere = "Смена не закрыта, а касса стоит на другой машине — закройте смену там",
    closeShiftAndDeregister = "Закрыть смену и снять с учёта",
    adminPin = "Пин администратора"
)
