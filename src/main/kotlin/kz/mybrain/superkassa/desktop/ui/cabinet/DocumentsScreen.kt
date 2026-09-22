package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentsOverview
import kz.mybrain.superkassa.desktop.server.cabinet.documentsOverview
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.JournalEmpty
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriod
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriodBar
import kz.mybrain.superkassa.desktop.ui.history.JournalQuery
import kz.mybrain.superkassa.desktop.ui.history.JournalSpan
import kz.mybrain.superkassa.desktop.ui.history.JournalView
import kz.mybrain.superkassa.desktop.ui.history.journalTypesIn
import kz.mybrain.superkassa.desktop.ui.history.presentIn
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Переход к документам кассы из её карточки.
 *
 * Экран документов стоит над кабинетом целиком, а кнопка перехода лежит
 * в карточке кассы — до неё от кабинета три вложения. Поэтому переход
 * отдаётся через окружение, как язык и словари, а не протягивается
 * обработчиком через каждый промежуточный экран.
 */
val LocalRegisterDocuments = staticCompositionLocalOf<(CabinetRegister) -> Unit> { {} }

/**
 * Документы кассы по данным ОФД — отдельным экраном.
 *
 * Кабинет показывает не то, что лежит в узле, а то, что принял сервер
 * приёма данных: расхождение между ними и есть главный смысл раздела.
 *
 * Тело экрана — тот же журнал, что и у кассы: поиск, отбор, порядок
 * строк, таблица и печать написаны один раз и взяты отсюда как есть.
 * Своего здесь только источник: виды документов кабинет отдаёт разными
 * списками, и выбор вида решает, какой список читать.
 *
 * Смену отсюда не открыть и не закрыть: касса из кабинета не управляется,
 * и кнопка, которой нечего сделать, хуже её отсутствия.
 *
 * Своего заголовка и своей стрелки возврата у экрана нет: и то и другое
 * стоит в шапке окна, одной на всё приложение.
 */
@Composable
fun CabinetDocumentsScreen(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister
) {
    val journal = journalTexts(session.language).history
    val scope = rememberCoroutineScope()
    var kind by remember(register.id) { mutableStateOf(DocumentKind.Receipts) }
    var period by remember(register.id) { mutableStateOf(JournalPeriod.of(JournalSpan.Week)) }
    var query by remember(register.id) { mutableStateOf(JournalQuery()) }
    var overview by remember(register.id) { mutableStateOf<DocumentsOverview?>(null) }
    val list = remember(register.id) { DocumentListState() }
    val desk = remember(register.id) { DocumentDesk(session, cabinet, texts) }
    // Открытый документ показывается вместо списка: возвращаться к нему
    // владелец будет по «Закрыть», а не поиском своего места в списке.
    var opened by remember(register.id) { mutableStateOf<OpenedDocument?>(null) }
    // Документ кабинет отдаёт отдельным обращением: нажатие на строку
    // не отзывалось ничем, пока оно шло.
    var opening by remember(register.id) { mutableStateOf(false) }

    // Чтение страницы одно на первый показ и на «показать ещё»: вид
    // документов и срок берутся здесь же, а не повторяются в двух местах.
    suspend fun read() {
        val token = cabinet.token ?: return
        list.loadNext(cabinet, token, register.id, kind, cabinetPeriodOf(period), texts)
    }

    LaunchedEffect(register.id, kind, period, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        opened = null
        opening = false
        overview = cabinet.guard { cabinet.client.documentsOverview(token, register.id) }
        list.reset()
        read()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        DocumentCounters(overview, texts, journal.spanAll)
        ChoiceSegments(
            options = DocumentKind.entries,
            selected = kind,
            label = { it.title(texts) },
            onSelect = { kind = it }
        )
        // У смен срока нет: кабинет отбором по дате их не отдаёт, и ряд
        // сегментов над списком обещал бы отбор, которого не будет.
        if (kind.dated) JournalPeriodBar(journal, period, list.loading) { period = it }
        val document = opened
        if (document != null || opening) {
            val state = if (document == null) ScreenState.Working else ScreenState.Ready
            ScreenSlot(state, Modifier.weight(1f)) {
                if (document != null) OpenedCard(document, texts) { opened = null }
            }
            return@Column
        }
        DocumentsJournal(
            journal = journal,
            empty = documentsEmpty(kind, period, overview, texts),
            list = list,
            query = query,
            desk = desk,
            registerId = register.id,
            kind = kind,
            onQuery = { query = it },
            onOpen = { target ->
                scope.launch {
                    opening = true
                    opened = openDocument(cabinet, register.id, kind, target)
                    opening = false
                }
            },
            onMore = { scope.launch { read() } }
        )
    }
}

/**
 * Тело экрана: тот же журнал, что и у кассы.
 *
 * Строки журнал показывает, не зная, откуда они пришли, и обратно
 * к записи кабинета ведёт ключ строки: по нему берётся то, что нужно
 * открыть или напечатать.
 */
@Composable
private fun ColumnScope.DocumentsJournal(
    journal: HistoryJournalTexts,
    empty: JournalEmpty,
    list: DocumentListState,
    query: JournalQuery,
    desk: DocumentDesk,
    registerId: String,
    kind: DocumentKind,
    onQuery: (JournalQuery) -> Unit,
    onOpen: (RowTarget?) -> Unit,
    onMore: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val entries = list.rows.map { it.entry }
    JournalView(
        journal = journal,
        entries = entries,
        types = journalTypesIn(entries),
        query = query.presentIn(entries),
        loading = list.loading,
        more = list.hasMore,
        empty = empty,
        onQuery = onQuery,
        onMore = onMore,
        onOpen = { entry -> onOpen(list.targetOf(entry.key)) },
        onPreview = { entry -> scope.launch { desk.preview(registerId, kind, entry, list.targetOf(entry.key)) } },
        onPrint = { entry -> scope.launch { desk.print(registerId, kind, list.targetOf(entry.key)) } }
    )
}

/** Раскрытый документ той карточкой, которая ему подходит. */
@Composable
private fun OpenedCard(document: OpenedDocument, texts: CabinetTexts, onClose: () -> Unit) {
    when (document) {
        is OpenedDocument.Receipt -> ReceiptCard(document.details, texts, onClose)
        is OpenedDocument.Report -> ReportCard(document.details, texts, onClose)
        is OpenedDocument.Movement -> CashMovementCard(document.details, texts, onClose)
        is OpenedDocument.Shift -> ShiftCard(document.shift, texts, onClose)
    }
}
