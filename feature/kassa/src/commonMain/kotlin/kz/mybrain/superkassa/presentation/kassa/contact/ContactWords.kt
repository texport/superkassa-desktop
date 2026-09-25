package kz.mybrain.superkassa.presentation.kassa.contact

import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactFieldTexts
import kz.mybrain.superkassa.strings.api.kassa.contact.ContactKindTexts

/** Название вида контакта на сегменте выбора. */
internal fun ContactKindTexts.of(kind: ContactKind): String = when (kind) {
    ContactKind.None -> none
    ContactKind.Phone -> phone
    ContactKind.Email -> email
    ContactKind.Telegram -> telegram
}

/**
 * Текст поля этого вида контакта.
 *
 * У вида «не отправлять» поля нет, и текста для него тоже: пусто.
 */
internal fun ContactFieldTexts.of(kind: ContactKind): String = when (kind) {
    ContactKind.None -> ""
    ContactKind.Phone -> phone
    ContactKind.Email -> email
    ContactKind.Telegram -> telegram
}
