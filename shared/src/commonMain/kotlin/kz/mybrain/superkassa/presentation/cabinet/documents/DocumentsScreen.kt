package kz.mybrain.superkassa.presentation.cabinet.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentKind
import kz.mybrain.superkassa.domain.cabinet.model.documents.OpenedDocument
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.documents.component.CashMovementCard
import kz.mybrain.superkassa.presentation.cabinet.documents.component.ReceiptCard
import kz.mybrain.superkassa.presentation.cabinet.documents.component.ReportCard
import kz.mybrain.superkassa.presentation.cabinet.documents.component.ShiftCard
import kz.mybrain.superkassa.presentation.common.document.JournalView
import kz.mybrain.superkassa.presentation.common.document.journalTypesIn
import kz.mybrain.superkassa.presentation.common.document.presentIn
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.period.JournalPeriodBar
import kz.mybrain.superkassa.presentation.common.picker.ChoiceSegments
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.print.preview.LocalPrint
import kz.mybrain.superkassa.presentation.print.preview.PrintActions
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.journal.HistoryJournalTexts
import kz.mybrain.superkassa.presentation.strings.journal.journalTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
    cabinet: CabinetViewModel,
    texts: CabinetTexts,
    register: CabinetRegister
) {
    val language = LocalLanguage.current
    val model = documentsViewModel(cabinet)
    val state by model.state.collectAsScreenState()
    LaunchedEffect(register.id) { model.show(register) }
    val journal = journalTexts(language).history
    val print = LocalPrint.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        DocumentCounters(state.overview, texts, journal.spanAll)
        ChoiceSegments(
            options = DocumentKind.entries,
            selected = state.kind,
            label = { it.title(texts) },
            onSelect = model::kind
        )
        // У смен срока нет: кабинет отбором по дате их не отдаёт, и ряд
        // сегментов над списком обещал бы отбор, которого не будет.
        if (state.kind.dated) JournalPeriodBar(journal, state.period, state.loading, model::period)
        DocumentsBody(journal, texts, state, model, print)
    }
}

/**
 * Что стоит под отбором: раскрытый документ, отказ кабинета или журнал.
 *
 * Открытый документ показывается вместо списка: возвращаться к нему
 * владелец будет по «Закрыть», а не поиском своего места в списке.
 * Отказ кабинета — не пустой список: о кассе, пробившей тысячу чеков,
 * «документов нет» говорит владельцу, что чеки потеряны.
 */
@Composable
private fun ColumnScope.DocumentsBody(
    journal: HistoryJournalTexts,
    texts: CabinetTexts,
    state: DocumentsUiState,
    model: DocumentsViewModel,
    print: PrintActions
) {
    val document = state.opened
    val trouble = state.trouble?.takeIf { state.rows.isEmpty() }
    when {
        document != null || state.opening -> {
            val slot = if (document == null) ScreenState.Working else ScreenState.Ready
            ScreenSlot(slot, Modifier.weight(1f)) {
                if (document != null) OpenedCard(document, texts, model::close)
            }
        }
        trouble != null -> ScreenSlot(ScreenState.Trouble(trouble, onRetry = model::more), Modifier.weight(1f)) {}
        else -> DocumentsJournal(journal, texts, state, model, print)
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
    texts: CabinetTexts,
    state: DocumentsUiState,
    model: DocumentsViewModel,
    print: PrintActions
) {
    val entries = state.rows.map { it.entry }
    JournalView(
        journal = journal,
        entries = entries,
        types = journalTypesIn(entries),
        query = state.query.presentIn(entries),
        loading = state.loading,
        more = state.hasMore,
        empty = documentsEmpty(state.kind, state.period, state.overview, texts),
        onQuery = model::query,
        onMore = model::more,
        onOpen = { entry -> model.open(entry.key) },
        onPreview = { entry -> model.form(entry, print::previewPacket) },
        onPrint = { entry -> model.form(entry) { packet, _, _ -> print.printPacket(packet) } }
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
