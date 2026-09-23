package kz.mybrain.superkassa.presentation.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.PAGE
import kz.mybrain.superkassa.data.node.documents
import kz.mybrain.superkassa.presentation.session.Session
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
