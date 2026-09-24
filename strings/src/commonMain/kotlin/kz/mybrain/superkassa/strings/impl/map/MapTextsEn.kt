package kz.mybrain.superkassa.strings.impl.map

import kz.mybrain.superkassa.strings.api.map.MapAddressTexts
import kz.mybrain.superkassa.strings.api.map.MapTexts

/** Надписи [MapTexts] по-английски. */
internal val mapTextsEn = MapTexts(
    pickOnMapHint = "Click the map where the place stands; drag the map with the mouse",
    pickPoint = "Take these coordinates",
    noTiles = "No map imagery: the tile service did not answer. The place is still set by clicking",
    showDegrees = "Find on the map",
    zoomIn = "Zoom in",
    zoomOut = "Zoom out",
    myLocation = "Show my city",
    myLocationShown = "The haloed dot is your city by connection address",
    myLocationPrecise = "The haloed dot is where you are, by this machine location service",
    findHouse = "Street address",
    locationAsk = "Detect the location?",
    locationAskHint = "The application will ask an external service which city your " +
        "connection address belongs to. Only the address itself leaves the machine — " +
        "neither the register nor the company is named. The accuracy is a city, not " +
        "a building: place the point itself by clicking the map.",
    locationAllow = "Allow",
    locationDeny = "Do not allow",
    address = MapAddressTexts(
        pickAddressFirst = "Pick the address in the registry — the map will find the building; " +
            "or place a marker and look the address up by it",
        markFirst = "First click the map where the place stands",
        searching = "Looking for this address on the map",
        notOnMap = "The map did not find this address — place the marker yourself by clicking",
        byPoint = "Address by marker",
        byPointSearching = "Finding out what place is under the marker",
        byPointHouses = "Registry buildings for this marker — pick yours",
        byPointNoPlace = "The map service did not recognise the place under the marker — pick the address step by step",
        byPointNoRegion = "The registry has no region the map named — pick the address step by step",
        byPointNoLocality = "The registry has no locality the map named — pick the address step by step",
        byPointNoStreet = "The registry has no street the map named — pick the address step by step",
        byPointNoHouse = "The registry has no building with this number — pick the address step by step"
    )
)
