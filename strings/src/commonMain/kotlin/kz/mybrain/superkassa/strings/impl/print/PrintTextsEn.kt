package kz.mybrain.superkassa.strings.impl.print

import kz.mybrain.superkassa.strings.api.print.PrintTexts

/** Надписи [PrintTexts] по-английски. */
internal val printTextsEn = PrintTexts(
    keepUnavailable = "This device cannot save the form to a file yet: open it on another machine",
    printerGone = "The register's printer is not found in the system: it is disconnected or removed. " +
        "Connect it or choose another printer in the settings",
    systemDialog = "Printing goes through the system dialog: the printer, the number of copies and " +
        "“Save as PDF” are chosen there"
)
