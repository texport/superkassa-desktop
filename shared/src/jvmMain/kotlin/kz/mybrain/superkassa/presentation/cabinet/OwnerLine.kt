package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.presentation.session.CabinetSession
import kz.mybrain.superkassa.presentation.strings.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.Glyphs

/**
 * Реквизит компании и вошедший — одной служебной строкой.
 *
 * Пустые части выпадают, а не оставляют висящие разделители: у только что
 * заведённой компании имени в кабинете может ещё не быть.
 */
fun ownerLine(cabinet: CabinetSession, texts: CabinetTexts): String = listOf(
    ownerIdentifier(cabinet, texts),
    cabinet.user?.fullName.orEmpty()
).filter { it.isNotBlank() }.joinToString(Glyphs.SEPARATOR)
