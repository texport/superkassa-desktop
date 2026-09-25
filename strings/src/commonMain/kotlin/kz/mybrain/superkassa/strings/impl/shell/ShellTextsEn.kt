package kz.mybrain.superkassa.strings.impl.shell

import kz.mybrain.superkassa.strings.api.shell.ShellTexts
import kz.mybrain.superkassa.strings.api.shell.StartFailureTexts

/** Надписи [ShellTexts] по-английски. */
internal val shellTextsEn = ShellTexts(
    title = "The cash register did not open",
    nodeRunning = StartFailureTexts(
        reason = "The previous cash register program (the node) is still running on this computer, " +
            "and its data cannot be moved while it writes to its database. Nothing was changed.",
        action = "Close the previous cash register program or restart the computer, then open the cash register again."
    ),
    kassaRunning = StartFailureTexts(
        reason = "The cash register is already open in another window on this computer.",
        action = "Switch to the open cash register window, or close it and open the cash register again."
    ),
    bothDatabases = StartFailureTexts(
        reason = "The cash register already has its own database, and the previous cash register program " +
            "(the node) left its data next to it. There is nowhere to move that data, and opening " +
            "the cash register without it would lose shifts and receipts. Nothing was changed.",
        action = "Call support: they will decide which data to keep. The details below will help them."
    ),
    nodeDataUnfit = StartFailureTexts(
        reason = "The data of the previous cash register program (the node) could not be moved without losses. " +
            "The cash register and the node data were left as they were.",
        action = "Call support and give them the details below and the application log."
    ),
    kassaNotOpened = StartFailureTexts(
        reason = "The cash register could not open its data folder.",
        action = "Check that the disk is available and has free space, then open the cash register again; " +
            "if that does not help, call support."
    ),
    forSupport = "Details for support",
    close = "Close"
)
