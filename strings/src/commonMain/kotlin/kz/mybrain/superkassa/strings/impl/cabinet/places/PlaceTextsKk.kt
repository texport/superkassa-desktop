package kz.mybrain.superkassa.strings.impl.cabinet.places

import kz.mybrain.superkassa.strings.api.cabinet.places.PlaceTexts
import kz.mybrain.superkassa.strings.impl.analytics.sieveTextsKk

/** Надписи [PlaceTexts] по-казахски. */
internal val placeTextsKk = PlaceTexts(
    title = "Сауда нүктелері",
    empty = "Сауда нүктелері жоқ — біріншісін қосыңыз",
    add = "Сауда нүктесін құру",
    name = "Нүкте атауы",
    address = "Мекенжай",
    place = "Сауда нүктесі",
    registerCount = "Кассалар",
    registers = "Кассалар",
    registersEmpty = "Кассалар жоқ — біріншісін қосыңыз",
    chooseRegister = "Кассаны таңдаңыз",
    pickRegisterFirst = "Касса таңдалмаған",
    rename = "Атын өзгерту",
    changeAddress = "Мекенжайды ауыстыру",
    addressChanged = "Нүктенің мекенжайы өзгертілді",
    addressNeedsReregistration = "Мекенжай өзгермеді: алдымен осы нүктенің кассаларын қайта тіркеңіз",
    exists = "Осы мекенжай мен орында нүкте бұрыннан бар, жаңасы құрылмады",
    removeBlocked = "Кассалары бар нүктені жоюға болмайды",
    latitude = "Ендік",
    longitude = "Бойлық",
    pickOnMap = "Картадағы орын",
    pointNotChosen = "Картадағы орын таңдалмаған",
    search = "Іздеу: нүкте, касса, МКК нөмірі",
    notFound = "Ештеңе табылмады",
    shownOf = "%2\$s ішінен %1\$s көрсетілді",
    sieve = sieveTextsKk,
    orderByName = "Атауы бойынша",
    orderByAddress = "Мекенжайы бойынша",
    orderByRegisters = "Касса саны бойынша",
    orderByRecord = "Күйі бойынша",
    ascending = "Өсу бойынша",
    descending = "Кему бойынша"
)
