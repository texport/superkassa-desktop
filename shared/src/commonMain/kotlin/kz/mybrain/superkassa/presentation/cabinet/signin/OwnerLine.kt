package kz.mybrain.superkassa.presentation.cabinet.signin

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Реквизит компании и вошедший — одной служебной строкой.
 *
 * Пустые части выпадают, а не оставляют висящие разделители: у только что
 * заведённой компании имени в кабинете может ещё не быть.
 */
fun ownerLine(owner: CabinetOwner, texts: CabinetTexts): String = listOf(
    ownerIdentifier(owner, texts),
    owner.user.fullName
).filter { it.isNotBlank() }.joinToString(Glyphs.SEPARATOR)
