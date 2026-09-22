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
 * Исход чтения различает пустой день и неудавшееся чтение: «за этот день
 * чеков нет» и «прочитать не удалось» — разные вещи, и кассир поступает
 * с ними по-разному.
 */
internal suspend fun loadDay(
    session: Session,
    what: String,
    day: LocalDate,
    into: MutableList<Document>
): JournalLoad {
    val kkm = session.selected ?: return JournalLoad.Failed
    val range = dayRange(day)
    val loaded = session.guard(what) {
        session.client.documents(kkm.kkmId, range.fromMillis, range.toMillis, session.pin, into.size)
    } ?: return JournalLoad.Failed
    into.addAll(loaded)
    return pageLoad(loaded.size, PAGE)
}
