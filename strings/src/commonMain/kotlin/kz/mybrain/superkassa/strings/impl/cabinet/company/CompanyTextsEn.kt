package kz.mybrain.superkassa.strings.impl.cabinet.company

import kz.mybrain.superkassa.strings.api.cabinet.company.CompanyTexts

/** Надписи [CompanyTexts] по-английски. */
internal val companyTextsEn = CompanyTexts(
    title = "Company",
    fromEds = "The company here is the one whose digital signature you signed " +
        "in with: the name and the details are issued by the KGD and are not editable. " +
        "Retail places, registers and applications all belong to it",
    okeds = "Activity codes",
    okedsEmpty = "No activity codes set",
    primaryOked = "Primary",
    saveOkeds = "Save activity codes",
    addOked = "Add an activity code",
    okedSearch = "Search the OKED classifier",
    okedNotFound = "Nothing found. The classifier knows only its own wording: a pharmacy there is trade in " +
        "pharmaceutical goods",
    okedNarrowSearch = "The classifier is longer than shown: “Show more” at the end of the list, or narrow the search",
    okedCode = "OKED code",
    okedName = "Activity name",
    showMore = "Show more"
)
