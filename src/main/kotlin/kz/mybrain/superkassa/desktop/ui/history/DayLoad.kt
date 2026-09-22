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
 * @return прочитан ли день и есть ли за пришедшей страницей ещё
 *   документы — два разных сведения, см. [PageOutcome].
 */
internal suspend fun loadDay(
    session: Session,
    what: String,
    day: LocalDate,
    into: MutableList<Document>
): PageOutcome {
    val kkm = session.selected ?: return PageOutcome.unread
    val range = dayRange(day)
    val loaded = session.guard(what) {
        session.client.documents(kkm.kkmId, range.fromMillis, range.toMillis, session.pin, into.size)
    } ?: return PageOutcome.unread
    // Чек, пробитый между двумя обращениями, сдвигает счёт страниц, и
    // в следующей приходит уже показанный документ: см. [newTo].
    into.addAll(loaded.newTo(into))
    return PageOutcome.page(loaded.size == PAGE)
}
