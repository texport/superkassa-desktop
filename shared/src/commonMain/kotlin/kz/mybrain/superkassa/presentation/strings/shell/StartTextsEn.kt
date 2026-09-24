package kz.mybrain.superkassa.presentation.strings.shell

internal val startTextsEn = StartTexts(
    title = "The cash register did not open",
    nodeRunning = StartWords(
        reason = "The previous cash register program (the node) is still running on this computer, " +
            "and its data cannot be moved while it writes to its database. Nothing was changed.",
        action = "Close the previous cash register program or restart the computer, then open the cash register again."
    ),
    kassaRunning = StartWords(
        reason = "The cash register is already open in another window on this computer.",
        action = "Switch to the open cash register window, or close it and open the cash register again."
    ),
    bothDatabases = StartWords(
        reason = "The cash register already has its own database, and the previous cash register program " +
            "(the node) left its data next to it. There is nowhere to move that data, and opening " +
            "the cash register without it would lose shifts and receipts. Nothing was changed.",
        action = "Call support: they will decide which data to keep. The details below will help them."
    ),
    nodeDataUnfit = StartWords(
        reason = "The data of the previous cash register program (the node) could not be moved without losses. " +
            "The cash register and the node data were left as they were.",
        action = "Call support and give them the details below and the application log."
    ),
    kassaNotOpened = StartWords(
        reason = "The cash register could not open its data folder.",
        action = "Check that the disk is available and has free space, then open the cash register again; " +
            "if that does not help, call support."
    ),
    forSupport = "Details for support",
    close = "Close"
)
