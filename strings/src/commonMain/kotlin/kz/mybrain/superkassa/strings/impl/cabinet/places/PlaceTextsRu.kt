package kz.mybrain.superkassa.strings.impl.cabinet.places

import kz.mybrain.superkassa.strings.api.cabinet.places.PlaceTexts
import kz.mybrain.superkassa.strings.impl.analytics.sieveTextsRu

/** Надписи [PlaceTexts] по-русски. */
internal val placeTextsRu = PlaceTexts(
    title = "Торговые точки",
    empty = "Торговых точек нет — создайте первую",
    add = "Создать торговую точку",
    name = "Название точки",
    address = "Адрес",
    place = "Торговая точка",
    registerCount = "Касс",
    registers = "Кассы",
    registersEmpty = "Касс нет — создайте первую",
    chooseRegister = "Выберите кассу",
    pickRegisterFirst = "Касса не выбрана",
    rename = "Переименовать",
    changeAddress = "Сменить адрес",
    addressChanged = "Адрес точки изменён",
    addressNeedsReregistration = "Адрес остался прежним: сначала перерегистрируйте кассы этой точки",
    exists = "По этому адресу и месту точка уже есть, новая не создана",
    removeBlocked = "Точку с кассами удалить нельзя",
    delete = "Удалить",
    deleteWhat = "Удалить точку «%1\$s»?",
    deleteExplain = "Точка исчезнет из кабинета БФД. Отменить это нельзя.",
    point = "Место на карте",
    latitude = "Широта",
    longitude = "Долгота",
    pickOnMap = "Место на карте",
    pointNotChosen = "Место на карте не выбрано",
    search = "Поиск: точка, касса, номер КГД",
    notFound = "Ничего не нашлось",
    shownOf = "Показано %1\$s из %2\$s",
    sieve = sieveTextsRu,
    orderByName = "По названию",
    orderByAddress = "По адресу",
    orderByRegisters = "По числу касс",
    orderByRecord = "По состоянию",
    ascending = "По возрастанию",
    descending = "По убыванию"
)
