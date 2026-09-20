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

@Composable
internal fun ColumnScope.ShiftDocuments(
    session: Session,
    journal: ShiftJournalTexts,
    shift: Shift,
    documents: List<Document>,
    loading: Boolean,
    onBack: () -> Unit,
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
    val state = when {
        documents.isNotEmpty() -> ScreenState.Ready
        loading -> ScreenState.Working
        else -> ScreenState.Empty(AppIcons.noDocuments, journal.emptyDocuments, journal.emptyDocumentsHint)
    }
    ScreenSlot(state, Modifier.weight(1f)) {
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

internal suspend fun loadDocuments(
    session: Session,
    what: String,
    shiftId: String,
    into: MutableList<Document>
) {
    val kkm = session.selected ?: return
    val loaded = session.guard(what) {
        session.client.documentsOfShift(kkm.kkmId, shiftId, session.pin)
    } ?: return
    into.clear()
    into.addAll(loaded)
}
