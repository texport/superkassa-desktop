package kz.mybrain.superkassa.desktop.ui.strings

/** Надписи учёта касс по-английски. Состав полей задан в [AnalyticsRecordTexts]. */
internal val analyticsRecordTextsEn = AnalyticsRecordTexts(
    tab = "Register record",

    title = "The fleet and its record",
    hint = "A register is added in the cabinet, but it is the KGD that puts it on the record " +
        "upon an application. Until the application is filed and granted, the register counts " +
        "as added, yet it may not trade by law",

    total = "Registers in all",
    places = "Retail places",

    trading = "Trading now",
    blocked = "Blocked",

    refusals = "KGD refusals",
    refusalsHint = "Registers the KGD refused to put on the record. The owner works through " +
        "the reason for each one and files again — they will not get on the record by themselves",
    refusalsNone = "No KGD refusals",

    regions = "Record by region",
    regionsHint = "The region is taken from the retail place address: a register has no region " +
        "field of its own. Regions go by the number of registers, the largest first"
)
