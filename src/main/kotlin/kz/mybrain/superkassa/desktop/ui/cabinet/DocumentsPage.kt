package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentsOverview
import kz.mybrain.superkassa.desktop.server.cabinet.documentsOverview
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.CounterTile
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Что доехало до ОФД по выбранной кассе.
 *
 * Кабинет показывает не то, что лежит в узле, а то, что принял сервер
 * приёма данных: расхождение между ними и есть главный смысл этого
 * раздела. Поэтому чек здесь назван состоянием доставки и отметкой КГД,
 * а не «пробит».
 *
 * Список читается страницами и за выбранный срок: за год работы кассы
 * чеков десятки тысяч, и «первые пятьдесят за всё время» показывали
 * позапрошлый месяц вместо сегодняшнего дня.
 */
@Composable
fun DocumentsPage(cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    var registerId by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(DocumentKind.Receipts) }
    var span by remember { mutableStateOf(DocumentSpan.Week) }
    var overview by remember { mutableStateOf<DocumentsOverview?>(null) }
    val list = remember { DocumentListState() }
    // Открытый документ показывается вместо списка: возвращаться к нему
    // владелец будет по «Закрыть», а не поиском своего места в списке.
    var opened by remember { mutableStateOf<OpenedDocument?>(null) }

    LaunchedEffect(cabinet.token) { cabinet.refreshRegisters() }

    LaunchedEffect(registerId, kind, span, cabinet.token) {
        val token = cabinet.token
        val id = registerId
        if (token == null || id == null) return@LaunchedEffect
        opened = null
        overview = cabinet.guard { cabinet.client.documentsOverview(token, id) }
        list.reset()
        list.loadNext(cabinet, token, id, kind, span, texts)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        LabelledPicker(
            label = texts.chooseRegister,
            options = cabinet.registers,
            selected = cabinet.registers.firstOrNull { it.id == registerId },
            title = { chosen -> chosen?.let { registerTitle(it) }.orEmpty() },
            onSelect = { registerId = it.id }
        )
        if (registerId == null) {
            EmptyState(AppIcons.kkm, texts.pickRegisterFirst, texts.pickRegisterFirstHint)
            return@Column
        }
        OverviewCard(overview, texts)
        ChoiceSegments(
            options = DocumentKind.entries,
            selected = kind,
            label = { it.title(texts) },
            onSelect = { kind = it }
        )
        DocumentSpanSegments(kind, span, texts) { span = it }
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.snug) {
            val document = opened
            if (document != null) {
                OpenedCard(document, texts) { opened = null }
                return@ScrollableColumn
            }
            DocumentList(list, kind, texts, onOpen = { row ->
                scope.launch { opened = openDocument(cabinet, registerId, kind, row) }
            }) {
                scope.launch {
                    val token = cabinet.token ?: return@launch
                    val id = registerId ?: return@launch
                    list.loadNext(cabinet, token, id, kind, span, texts)
                }
            }
        }
    }
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

/**
 * Сколько чего у кассы накопилось по данным ОФД.
 *
 * Счётчики стояли строками «подпись — значение» в ряд и читались как
 * реквизиты карточки. Число здесь и есть содержание, поэтому оно набрано
 * крупно, а подпись под ним.
 */
@Composable
private fun OverviewCard(overview: DocumentsOverview?, texts: CabinetTexts) {
    val counts = overview ?: return
    SectionCard(title = texts.documents) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CounterTile(counts.receiptsCount.toString(), texts.receipts)
            CounterTile(counts.shiftsCount.toString(), texts.shifts)
            CounterTile(counts.reportsCount.toString(), texts.reports)
            CounterTile(counts.cashMovementsCount.toString(), texts.cashMovements)
        }
    }
}

/**
 * Список документов выбранного вида.
 *
 * Сколько показано из скольких сказано под списком, рядом с кнопкой
 * подгрузки: у страничного списка одно число ниоткуда не говорит, весь
 * это срок или его начало, — а в заголовке оно читалось как оторванная
 * от всего цифра.
 */
@Composable
private fun DocumentList(
    list: DocumentListState,
    kind: DocumentKind,
    texts: CabinetTexts,
    onOpen: (DocumentRow) -> Unit,
    onMore: () -> Unit
) {
    SectionCard(title = kind.title(texts)) {
        if (list.rows.isEmpty()) {
            EmptyState(AppIcons.history, texts.documentsEmpty, texts.documentsEmptyHint)
            return@SectionCard
        }
        if (kind == DocumentKind.Receipts) {
            Text(
                text = texts.receiptsHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        list.rows.forEachIndexed { at, row ->
            RecordRowOf(row, texts, at % STRIPE == 1) { onOpen(row) }
        }
        MoreRow(
            more = list.hasMore,
            loading = list.loading,
            showMore = texts.showMore,
            allShown = texts.allShown,
            note = texts.shownOf.format(list.rows.size, list.total),
            onMore = onMore
        )
    }
}

/** Затеняется каждая вторая строка списка. */
private const val STRIPE = 2
