package kz.mybrain.superkassa.strings.impl.cabinet.eds

import kz.mybrain.superkassa.strings.api.cabinet.eds.EdsTexts

/** Надписи [EdsTexts] по-английски. */
internal val edsTextsEn = EdsTexts(
    remaining = "%s left",
    cancelWait = "Stop waiting",
    ncaLayer = "NCALayer",
    egov = "eGov mobile",
    keyFile = "Key file",
    aboutEgov = "Sign in the eGov mobile app: on this device or by scanning a QR code with your phone. The key stays " +
        "in eGov mobile",
    aboutKeyFile = "Sign with a .p12 key file on this device. The password is entered for every signature and never " +
        "stored",
    waitEgov = "Sign in eGov mobile: scan the QR code with your phone or open the app on this device",
    waitKeyFile = "Choose the key file and enter its password",
    egovTitle = "Sign in eGov mobile",
    egovScan = "Scan the QR code in eGov mobile on your phone or open the app on this device",
    egovOpen = "Open eGov mobile",
    egovNotOpened = "Could not open eGov mobile on this device. Scan " +
        "the QR code with a phone that has eGov mobile",
    egovQr = "QR code for signing in eGov mobile",
    egovDocument = "Signature for the BFD cabinet — Superkassa",
    keyTitle = "Sign with a key file",
    keyOther = "Another file",
    keyPassword = "Key password",
    showPassword = "Show password",
    hidePassword = "Hide password",
    keySign = "Sign",
    cancel = "Cancel",
    wrongPassword = "Wrong password. Check the keyboard layout and try again",
    keyUnreadable = "This is not a digital signature key file or it is damaged. Choose a .p12 file",
    keyNotForSigning = "This key is for authentication (AUTH). Choose the signing key — the file without AUTH in its " +
        "name",
    keyExpired = "The key certificate has expired. Choose a valid key",
    egovUnreachable = "The eGov mobile signing service does not answer. Check the internet connection and try again",
    egovExpired = "Time to sign in eGov mobile is over. Try again",
    cancelled = "Signing cancelled"
)
