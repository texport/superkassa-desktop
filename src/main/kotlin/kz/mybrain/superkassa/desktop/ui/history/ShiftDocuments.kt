package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ShiftJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Документы одной смены.
 *
 * @param page чем кончилось чтение документов: узел, который отказал или
 *   промолчал, о документах смены ничего не сказал, и «документов нет»
 *   про смену с сотней чеков — неправда.
 * @param onRetry перечитать документы после неудачи.
 */
@Composable
internal fun ColumnScope.ShiftDocuments(
    session: Session,
    journal: ShiftJournalTexts,
    shift: Shift,
    documents: List<Document>,
    loading: Boolean,
    page: PageOutcome,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPreview: (Document) -> Unit
) {
    val texts = LocalStrings.current
    val history = journalTexts(session.language).history
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(AppIcons.earlierDay, contentDescription = journal.back)
        }
        Text(
            text = "${journal.number} ${shift.shiftNo ?: Glyphs.DASH}${Glyphs.SEPARATOR}${journal.documents}",
            style = MaterialTheme.typography.titleMedium
        )
    }
    ScreenSlot(shiftDocumentsState(journal, documents.size, loading, page, onRetry), Modifier.weight(1f)) {
        // Та же таблица, что и в журнале за срок: документы смены — те же
        // документы, и вторая разметка под них разошлась бы с первой.
        val entries = journalEntriesOf(session, texts, documents)
        JournalHeader(history)
        ScrollableList(modifier = Modifier.weight(1f)) {
            itemsIndexed(entries, key = { _, entry -> entry.key }) { at, entry ->
                val document = documents.firstOrNull { it.id == entry.key }
                JournalRow(
                    entry = entry,
                    striped = stripedAt(at),
                    onPreview = { document?.let(onPreview) },
                    onPrint = { document?.let(session.printDesk::print) }
                )
            }
        }
    }
}

/**
 * Что стоит на месте списка документов смены.
 *
 * «В этой смене документов нет» — утверждение о смене, и говорить его
 * можно только вслед за ответом узла. Узел, который отказал или промолчал,
 * о документах смены не сказал ничего: кассир, пришедший за чеком
 * позавчерашней смены, читал его молчание как пустую смену.
 */
internal fun shiftDocumentsState(
    journal: ShiftJournalTexts,
    documents: Int,
    loading: Boolean,
    page: PageOutcome,
    onRetry: () -> Unit
): ScreenState = when {
    documents > 0 -> ScreenState.Ready
    loading -> ScreenState.Working
    !page.read -> ScreenState.Trouble(journal.documentsUnread, journal.documentsUnreadHint, onRetry)
    else -> ScreenState.Empty(AppIcons.noDocuments, journal.emptyDocuments, journal.emptyDocumentsHint)
}

/**
 * Читает документы смены.
 *
 * @return прочитаны ли они; за одно обращение узел отдаёт смену целиком,
 *   и дочитывать здесь нечего.
 */
internal suspend fun loadDocuments(
    session: Session,
    what: String,
    shiftId: String,
    into: MutableList<Document>
): PageOutcome {
    val kkm = session.selected ?: return PageOutcome.unread
    val loaded = session.guard(what) {
        session.client.documentsOfShift(kkm.kkmId, shiftId, session.pin)
    } ?: return PageOutcome.unread
    into.clear()
    into.addAll(loaded)
    return PageOutcome.page(more = false)
}
