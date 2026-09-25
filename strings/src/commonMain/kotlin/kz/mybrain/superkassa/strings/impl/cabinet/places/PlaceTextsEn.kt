package kz.mybrain.superkassa.strings.impl.cabinet.places

import kz.mybrain.superkassa.strings.api.cabinet.places.PlaceTexts
import kz.mybrain.superkassa.strings.impl.analytics.sieveTextsEn

/** Надписи [PlaceTexts] по-английски. */
internal val placeTextsEn = PlaceTexts(
    title = "Retail places",
    empty = "No retail places yet — add the first one",
    add = "Create a retail place",
    name = "Place name",
    address = "Address",
    place = "Retail place",
    registerCount = "Registers",
    registers = "Cash registers",
    registersEmpty = "No cash registers yet — add the first one",
    chooseRegister = "Choose a register",
    pickRegisterFirst = "No register chosen",
    rename = "Rename",
    changeAddress = "Change the address",
    addressChanged = "The address has been changed",
    addressNeedsReregistration = "The address is unchanged: re-register the cash registers of this place first",
    exists = "A place already exists at this address and location; no new one was created",
    removeBlocked = "A place with registers cannot be removed",
    latitude = "Latitude",
    longitude = "Longitude",
    pickOnMap = "Place on the map",
    pointNotChosen = "No place on the map chosen",
    search = "Search: place, register, KGD number",
    notFound = "Nothing found",
    shownOf = "Shown %1\$s of %2\$s",
    sieve = sieveTextsEn,
    orderByName = "By name",
    orderByAddress = "By address",
    orderByRegisters = "By register count",
    orderByRecord = "By record state",
    ascending = "Ascending",
    descending = "Descending"
)
