package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.EnumStrings

/**
 * Название вида документа для кассира.
 *
 * Сначала справочник кассы, затем свои слова: в записях прежних версий
 * встречаются виды, которых справочник не знает, — «CHECK», «REPORT_X», —
 * а голый код кассиру показывать нельзя.
 */
fun documentTypeTitle(
    code: String?,
    names: Map<String, TrilingualMessageResponse>,
    language: Language,
    enums: EnumStrings
): String {
    val known = code ?: return Glyphs.DASH
    return names[known]?.of(language) ?: enums.documentFallback(known) ?: known
}

/**
 * Виды документов, которые встретились в журнале, в порядке справочника кассы.
 *
 * Перечень строится по тому, что действительно есть за срок: предлагать
 * отбор по виду, которого за срок не было, значит показывать пустой список
 * без объяснения. Вид, которого в справочнике нет, идёт в конец, но
 * не теряется.
 */
fun documentTypesIn(codes: List<String>, order: List<String>): List<String> =
    codes.distinct().sortedWith(compareBy({ order.indexOf(it).takeIf { at -> at >= 0 } ?: order.size }, { it }))
