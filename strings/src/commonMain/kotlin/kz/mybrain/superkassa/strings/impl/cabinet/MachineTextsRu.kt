package kz.mybrain.superkassa.strings.impl.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.MachineTexts

/** Надписи [MachineTexts] по-русски. */
internal val machineTextsRu = MachineTexts(
    title = "Работа на этой машине",
    worksHere = "Касса заведена на этой машине",
    goToKkm = "Перейти к кассе",
    onlyOnRecord = "Работать можно только с кассы, стоящей на учёте в КГД",
    notHere = "На этой машине касса не заведена",
    workHere = "Работать на этой машине",
    tokenReissued = "Токен будет выпущен заново: действующий кабинет показать не умеет. " +
        "Прежний токен перестанет действовать.",
    heardByOfd = "БФД принимала данные от этой кассы",
    handoverUnderstood = "Понимаю: на другой машине эта касса работать перестанет",
    done = "Касса заведена — кассир входит в неё по этому пину администратора",
    stranded = "Токен выпущен, а касса на этой машине не заведена. Повторите: уйдёт тот же " +
        "токен, второй раз он не выпускается.",
    retry = "Повторить"
)
