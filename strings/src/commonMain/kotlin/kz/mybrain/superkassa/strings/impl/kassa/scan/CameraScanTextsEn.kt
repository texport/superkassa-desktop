package kz.mybrain.superkassa.strings.impl.kassa.scan

import kz.mybrain.superkassa.strings.api.kassa.scan.CameraScanTexts

/** Надписи [CameraScanTexts] по-английски. */
internal val cameraScanTextsEn = CameraScanTexts(
    scan = "Scan with the camera",
    title = "Barcode scanner",
    close = "Close",
    hint = "Point the camera at the item barcode — the code is found by itself",
    why = "The register reads barcodes with the device camera. Pictures are not kept and go nowhere.",
    allow = "Allow the camera",
    denied = "Camera access is denied. Turn it on in the app settings: Permissions → Camera.",
    settings = "Open settings"
)
