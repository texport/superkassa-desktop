package kz.mybrain.superkassa.desktop.ui.history

import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings

/**
 * Типы документов, которые встретились в журнале, в порядке справочника узла.
 *
 * Перечень строится по тому, что действительно есть за день: предлагать
 * кассиру отбор по типу, которого в этот день не было, значит показывать
 * ему пустой список без объяснения. Тип, которого в справочнике нет —
 * например, снятый с учёта «CHECK» из прежних записей, — идёт в конец,
 * но не теряется.
 */
fun documentTypesIn(documents: List<Document>, order: List<String>): List<String> =
    documents.mapNotNull { it.docType }
        .distinct()
        .sortedWith(compareBy({ order.indexOf(it).takeIf { at -> at >= 0 } ?: order.size }, { it }))

/** Отбор по типу документа. Пустой отбор означает «все типы». */
fun filterByType(documents: List<Document>, type: String?): List<Document> =
    if (type == null) documents else documents.filter { it.docType == type }

/**
 * Название типа документа для кассира.
 *
 * Берётся из справочника узла. Исключение одно: тип «CHECK» узел больше
 * не отдаёт — он разделён на продажу, покупку и возвраты, — но в записях
 * прежних версий он ещё встречается, и справочник о нём ничего не знает.
 * Показывать в этом случае голый код нельзя, поэтому берётся давно
 * заведённое название чека.
 */
fun documentTypeTitle(session: Session, texts: AppStrings, code: String?): String {
    val title = session.titleOf(Dictionary.DocumentTypes, code)
    return if (title == LEGACY_CHECK) texts.enums.docCheck else title
}

/** Тип документа прежних версий узла: не разделял продажу и покупку. */
const val LEGACY_CHECK = "CHECK"
