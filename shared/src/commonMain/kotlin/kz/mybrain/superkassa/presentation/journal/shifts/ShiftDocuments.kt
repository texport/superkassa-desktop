package kz.mybrain.superkassa.presentation.journal.shifts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.list.MoreRow
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.document.JournalTable
import kz.mybrain.superkassa.presentation.common.period.text
import kz.mybrain.superkassa.presentation.common.print.PrintActions
import kz.mybrain.superkassa.presentation.journal.PageOutcome
import kz.mybrain.superkassa.presentation.journal.documents.journalEntriesOf
import kz.mybrain.superkassa.presentation.journal.documents.of
import kz.mybrain.superkassa.strings.api.journal.ShiftJournalTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Документы одной смены.
 *
 * Касса отдаёт их страницами, как и журнал за срок: смена оживлённой кассы
 * длиннее одной страницы, и её конец иначе молча пропадал бы.
 */
@Composable
internal fun ColumnScope.ShiftDocuments(
    journal: ShiftJournalTexts,
    state: ShiftsUiState,
    actions: ShiftsActions,
    print: PrintActions
) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    val history = textsOf(language).journal.history
    ShiftHeading(journal, state, actions)
    val page = state.documentsPage
    val slot = shiftDocumentsState(journal, state.documents.size, state.opening, page, actions::moreDocuments)
    ScreenSlot(slot, Modifier.weight(1f)) {
        // Та же таблица, что и в журнале за срок: документы смены — те же
        // документы, и вторая разметка под них разошлась бы с первой.
        val entries = remember(state.documents, state.documentTypes, language) {
            journalEntriesOf(texts, language, state.documentTypes, state.documents)
        }
        val printing = print.of(state.documents)
        JournalTable(history, entries, Modifier.weight(1f), onPreview = printing.preview, onPrint = printing.print)
        MoreRow(page.more, state.opening, history.showMore, history.allShown, onMore = actions::moreDocuments)
    }
}

/** Возврат к списку смен и номер открытой смены. */
@Composable
private fun ShiftHeading(journal: ShiftJournalTexts, state: ShiftsUiState, actions: ShiftsActions) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { actions.open(null) }) {
            Icon(AppIcons.earlierDay, contentDescription = journal.back)
        }
        Text(
            text = "${journal.number} ${state.opened?.shiftNo ?: Glyphs.DASH}${Glyphs.SEPARATOR}${journal.documents}",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Что стоит на месте списка документов смены.
 *
 * «В этой смене документов нет» — утверждение о смене, и говорить его
 * можно только вслед за ответом кассы. Касса, которая отказала или
 * промолчала, о документах смены не сказала ничего: кассир, пришедший
 * за чеком позавчерашней смены, читал её молчание как пустую смену.
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
