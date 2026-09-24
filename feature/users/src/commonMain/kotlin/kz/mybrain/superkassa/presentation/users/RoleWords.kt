package kz.mybrain.superkassa.presentation.users

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.UserStrings

/**
 * Название роли на языке кассира.
 *
 * Сначала слова кассы, потом свои: справочник ролей приходит отдельным
 * обращением, и до ответа в строке кассира стоял бы код «ADMIN» латиницей.
 */
internal fun roleWord(
    role: UserRole,
    texts: UserStrings,
    names: Map<String, TrilingualMessageResponse>,
    language: Language
): String = names[role.name]?.of(language) ?: when (role) {
    UserRole.ADMIN -> texts.admin
    UserRole.CASHIER -> texts.cashier
}
