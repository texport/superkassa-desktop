package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.PAGE
import kz.mybrain.superkassa.desktop.server.documents
import java.time.LocalDate

/**
 * Читает документы одного дня страницами.
 *
 * Общая для журнала и для выбора чека-основания на возврате: покупатель
 * приходит с чеком позавчерашнего дня, и искать его обе стороны обязаны
 * одинаково.
 *
 * Возвращает `true`, когда страница пришла полной, — значит день читается
 * дальше.
 */
internal suspend fun loadDay(
    session: Session,
    what: String,
    day: LocalDate,
    into: MutableList<Document>
): Boolean {
    val kkm = session.selected ?: return false
    val range = dayRange(day)
    val loaded = session.guard(what) {
        session.client.documents(kkm.kkmId, range.fromMillis, range.toMillis, session.pin, into.size)
    } ?: return false
    into.addAll(loaded)
    return loaded.size == PAGE
}
