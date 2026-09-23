package kz.mybrain.superkassa.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.documentsOfShift
import kz.mybrain.superkassa.domain.shift.Shift
import kz.mybrain.superkassa.presentation.components.ScreenSlot
import kz.mybrain.superkassa.presentation.components.ScreenState
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.strings.ShiftJournalTexts
import kz.mybrain.superkassa.presentation.strings.journalTexts
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Glyphs
import kz.mybrain.superkassa.presentation.theme.Spacing

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
        fun documentOf(entry: JournalEntry) = documents.firstOrNull { it.id == entry.key }
        JournalTable(
            journal = history,
            entries = entries,
            modifier = Modifier.weight(1f),
            onPreview = { entry -> documentOf(entry)?.let(onPreview) },
            onPrint = { entry -> documentOf(entry)?.let(session.printDesk::print) }
        )
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
