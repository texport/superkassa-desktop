package kz.mybrain.superkassa.strings.impl.update

import kz.mybrain.superkassa.strings.api.update.UpdateTexts

/** Надписи [UpdateTexts] по-английски. */
internal val updateTextsEn = UpdateTexts(
    appName = "Superkassa",
    title = "Updates",
    hint = "Once a day the till asks whether a new version is out. It installs nothing itself: " +
        "it downloads and checks the installer on request, and the owner installs it once the shift is closed",
    installed = "Installed version",
    automatic = "Check for updates automatically",
    lastChecked = "Last checked",
    neverChecked = "never checked",
    checkNow = "Check now",
    checking = "Checking…",
    upToDate = "The latest version is installed",
    available = "Version available",
    availableHint = "The till downloads the installer and checks it against the release: " +
        "one that does not match will not open. Install the new version with the shift closed: " +
        "the till closes, while its data stay in place.",
    download = "Download",
    downloading = "Downloading and checking…",
    later = "Later",
    unreachable = "Could not check: no connection",
    installerOpened = "The installer is downloaded, matches the release and is open: " +
        "install the new version with the shift closed",
    pageOpened = "There is nothing to check the installer against: the release page is open, download the file there",
    installerTampered = "The downloaded installer did not match the release and was removed: " +
        "it must not be installed. Try again later",
    installPermission = "Allow the register to install updates: in the settings that opened, turn on " +
        "“Allow from this source”, come back and press Download again",
    development = "Development build: releases are not installed over it — it was built without a release " +
        "tag and signed with another key"
)
