package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.ShiftJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Прошлые смены и их документы.
 *
 * Смены узел отдаёт отдельно от журнала за сутки, и до сих пор приложение
 * их не спрашивало: Z-отчёт закрытой позавчера смены нельзя было ни найти,
 * ни напечатать.
 */
@Composable
fun PastShiftsView(session: Session) {
    val journal = journalTexts(session.language).shifts
    val shifts = remember { mutableStateListOf<Shift>() }
    val documents = remember { mutableStateListOf<Document>() }
    var openId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session.selected?.kkmId) {
        shifts.clear()
        loading = true
        more = loadShifts(session, journal.title, shifts)
        loading = false
    }
    LaunchedEffect(openId) {
        documents.clear()
        val shiftId = openId ?: return@LaunchedEffect
        loadDocuments(session, journal.documents, shiftId, documents)
    }

    val opened = shifts.firstOrNull { it.id == openId }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        Text(
            text = journal.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (opened == null) {
            ShiftList(
                journal = journal,
                shifts = shifts,
                loading = loading,
                more = more,
                onMore = {
                    scope.launch {
                        loading = true
                        more = loadShifts(session, journal.title, shifts)
                        loading = false
                    }
                },
                onOpen = { openId = it.id }
            ) { shift ->
                session.printDesk.previewDocument(shift.zReportId)
            }
        } else {
            ShiftDocuments(session, journal, opened, documents, onBack = { openId = null }) { document ->
                session.printDesk.preview(document)
            }
        }
    }
}

@Composable
private fun ColumnScope.ShiftList(
    journal: ShiftJournalTexts,
    shifts: List<Shift>,
    loading: Boolean,
    more: Boolean,
    onMore: () -> Unit,
    onOpen: (Shift) -> Unit,
    onZReport: (Shift) -> Unit
) {
    if (loading) {
        JournalLoading(Modifier.weight(1f))
        return
    }
    if (shifts.isEmpty()) {
        EmptyState(AppIcons.noDocuments, journal.none, journal.noneHint, Modifier.weight(1f))
        return
    }
    ScrollableList(modifier = Modifier.weight(1f)) {
        itemsIndexed(shifts) { at, shift ->
            ShiftRow(journal, shift, at % 2 == 1, { onOpen(shift) }) { onZReport(shift) }
        }
    }
    // Под списком видно, кончились ли смены: молчание внизу не отличает
    // «всё» от «оборвалось на двухсотой».
    MoreRow(more, loading, journal.showMore, journal.allShown, onMore = onMore)
}

/**
 * Строка смены.
 *
 * Номер смены — заголовок, время открытия и закрытия — подпись под ним:
 * так строка читается как запись журнала, а не как ряд из трёх колонок,
 * в которых непонятно, что к чему относится.
 *
 * Кнопка Z-отчёта показана только у закрытой смены: у открытой отчёта
 * ещё нет, и нажатие дало бы отказ узла вместо бумаги.
 */
@Composable
private fun ShiftRow(
    journal: ShiftJournalTexts,
    shift: Shift,
    striped: Boolean,
    onOpen: () -> Unit,
    onZReport: () -> Unit
) {
    RecordRow(
        title = "${journal.number} ${shift.shiftNo ?: DASH}",
        subtitle = shiftMoments(journal, shift),
        striped = striped,
        onClick = onOpen,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    text = if (shift.isClosed) journal.closed else journal.stillOpen,
                    color = if (shift.isClosed) StatusColors.delivered else StatusColors.pending
                )
                if (shift.zReportId != null) {
                    TextButton(onClick = onZReport) { Text(journal.zReport) }
                }
            }
        }
    )
}

/** Время смены одной строкой: открыта тогда-то, закрыта тогда-то. */
private fun shiftMoments(journal: ShiftJournalTexts, shift: Shift): String {
    val opened = "${journal.opened}: ${momentText(shift.openedAt)}"
    val closed = if (shift.isClosed) "${journal.closed}: ${momentText(shift.closedAt)}" else journal.stillOpen
    return "$opened · $closed"
}

@Composable
private fun ColumnScope.ShiftDocuments(
    session: Session,
    journal: ShiftJournalTexts,
    shift: Shift,
    documents: List<Document>,
    onBack: () -> Unit,
    onPrint: (Document) -> Unit
) {
    val history = journalTexts(session.language).history
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(AppIcons.earlierDay, contentDescription = journal.back)
        }
        Text(
            text = "${journal.number} ${shift.shiftNo ?: DASH} · ${journal.documents}",
            style = MaterialTheme.typography.titleMedium
        )
    }
    if (documents.isEmpty()) {
        EmptyState(
            icon = AppIcons.noDocuments,
            title = journal.emptyDocuments,
            hint = journal.emptyDocumentsHint,
            modifier = Modifier.weight(1f)
        )
        return
    }
    DocumentJournalHeader(history)
    ScrollableList(modifier = Modifier.weight(1f)) {
        itemsIndexed(documents) { at, document ->
            DocumentJournalRow(
                session = session,
                document = document,
                striped = at % 2 == 1,
                onPreview = { onPrint(document) },
                onPrint = { session.printDesk.print(document) }
            )
        }
    }
}

/**
 * Дочитывает смены с того места, где остановились.
 *
 * @return есть ли за пришедшей страницей ещё смены.
 */
private suspend fun loadShifts(session: Session, what: String, into: MutableList<Shift>): Boolean {
    val kkm = session.selected ?: return false
    val loaded = session.guard(what) { session.client.shifts(kkm.kkmId, session.pin, into.size) }
        ?: return false
    into.addAll(loaded)
    return loaded.size == SHIFT_PAGE
}

private suspend fun loadDocuments(
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
