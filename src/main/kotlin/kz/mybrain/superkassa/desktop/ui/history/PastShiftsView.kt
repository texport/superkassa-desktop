package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.PrintFileName
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.ShiftJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
    // Чтение начинается вместе с экраном, поэтому ожидание стоит с первого
    // кадра: иначе пустой список успевал мелькнуть объяснением пустоты.
    var loading by remember { mutableStateOf(true) }
    // Чем кончилось чтение смен: прочитаны ли они и есть ли за страницей ещё.
    var page by remember { mutableStateOf(PageOutcome.unread) }
    val scope = rememberCoroutineScope()

    suspend fun read() {
        loading = true
        page = loadShifts(session, journal.title, shifts, page)
        loading = false
    }

    LaunchedEffect(session.selected?.kkmId) {
        shifts.clear()
        page = PageOutcome.unread
        read()
    }
    // Документы смены узел отдаёт отдельным обращением: до ответа список
    // пуст, и «документов нет» про смену с сотней чеков — неправда.
    // Неудача этого обращения тоже не пустая смена, поэтому её итог
    // хранится наравне с самими документами.
    var opening by remember { mutableStateOf(false) }
    var documentsPage by remember { mutableStateOf(PageOutcome.unread) }

    suspend fun readDocuments(shiftId: String) {
        opening = true
        documentsPage = loadDocuments(session, journal.documents, shiftId, documents)
        opening = false
    }

    LaunchedEffect(openId) {
        documents.clear()
        documentsPage = PageOutcome.unread
        openId?.let { readDocuments(it) }
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
                page = page,
                onMore = { scope.launch { read() } },
                onRetry = { scope.launch { read() } },
                // Ожидание ставится вместе с открытием смены: иначе
                // до первого кадра чтения экран успевал объяснить пустоту
                // смены, которую ещё никто не спрашивал.
                onOpen = {
                    openId = it.id
                    opening = true
                }
            ) { shift ->
                session.printDesk.previewDocument(
                    shift.zReportId,
                    PrintFileName.of(Z_REPORT, number = null, shiftNo = shift.shiftNo)
                )
            }
        } else {
            ShiftDocuments(
                session = session,
                journal = journal,
                shift = opened,
                documents = documents,
                loading = opening,
                page = documentsPage,
                onBack = { openId = null },
                onRetry = { scope.launch { readDocuments(opened.id) } },
                onPreview = { document -> session.printDesk.preview(document) }
            )
        }
    }
}

@Composable
private fun ColumnScope.ShiftList(
    journal: ShiftJournalTexts,
    shifts: List<Shift>,
    loading: Boolean,
    page: PageOutcome,
    onMore: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (Shift) -> Unit,
    onZReport: (Shift) -> Unit
) {
    val state = shiftsState(journal, shifts.size, loading, page, onRetry)
    ScreenSlot(state, Modifier.weight(1f)) {
        ScrollableList(modifier = Modifier.weight(1f)) {
            itemsIndexed(shifts) { at, shift ->
                ShiftRow(journal, shift, stripedAt(at), { onOpen(shift) }) { onZReport(shift) }
            }
        }
        // Под списком видно, кончились ли смены: молчание внизу не отличает
        // «всё» от «оборвалось на двухсотой».
        MoreRow(page.more, loading, journal.showMore, journal.allShown, onMore = onMore)
    }
}

/**
 * Что стоит на месте списка смен.
 *
 * «Смен нет» — утверждение о кассе, и говорить его можно только вслед
 * за ответом узла. Узел, который отказал или промолчал, о сменах ничего
 * не сказал: кассир, пришедший за Z-отчётом позавчерашней смены, читал
 * его молчание как утрату смены.
 */
internal fun shiftsState(
    journal: ShiftJournalTexts,
    shifts: Int,
    loading: Boolean,
    page: PageOutcome,
    onRetry: () -> Unit
): ScreenState = when {
    // Ожидание встаёт на место списка только до первого ответа: прочитанные
    // смены остаются на экране, пока читается следующая страница.
    loading && shifts == 0 -> ScreenState.Working
    // Прочитанные прежде смены остаются на месте: неудача дочитывания
    // не повод убирать с экрана то, что кассир уже видит.
    !page.read && shifts == 0 -> ScreenState.Trouble(journal.unread, journal.unreadHint, onRetry)
    shifts == 0 -> ScreenState.Empty(AppIcons.noDocuments, journal.none, journal.noneHint)
    else -> ScreenState.Ready
}

/**
 * Дочитывает смены с того места, где остановились.
 *
 * @param previous чем кончилось прошлое чтение: неудача дочитывания
 *   не отменяет того, что за прочитанными сменами есть ещё.
 * @return прочитаны ли смены и есть ли за пришедшей страницей ещё —
 *   два разных сведения, см. [PageOutcome].
 */
internal suspend fun loadShifts(
    session: Session,
    what: String,
    into: MutableList<Shift>,
    previous: PageOutcome = PageOutcome.unread
): PageOutcome {
    val kkm = session.selected ?: return PageOutcome.unreadAfter(previous)
    val loaded = session.guard(what) { session.client.shifts(kkm.kkmId, session.pin, into.size) }
        ?: return PageOutcome.unreadAfter(previous)
    // Смена, открытая между двумя обращениями, сдвигает счёт страниц,
    // и в следующей приходит уже показанная смена.
    val already = into.mapTo(mutableSetOf()) { it.id }
    into.addAll(loaded.filterNot { it.id in already })
    return PageOutcome.page(loaded.size == SHIFT_PAGE)
}

/** Вид документа закрытия смены, как его называет узел. */
private const val Z_REPORT = "SHIFT_CLOSE"
