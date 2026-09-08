package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Что доехало до ОФД по этой кассе.
 *
 * Кабинет показывает не то, что лежит в узле, а то, что принял сервер
 * приёма данных: расхождение между ними и есть главный смысл раздела.
 * Поэтому чек здесь назван состоянием доставки и отметкой КГД,
 * а не «пробит».
 *
 * Список читается страницами и за выбранный срок: за год работы кассы
 * чеков десятки тысяч, и «первые пятьдесят за всё время» показывали
 * позапрошлый месяц вместо сегодняшнего дня.
 *
 * Своего раздела у документов больше нет: они принадлежат кассе, и
 * выбирать её вторым списком после того, как она уже открыта, владельцу
 * было незачем.
 */
@Composable
fun RegisterDocuments(cabinet: CabinetSession, texts: CabinetTexts, registerId: String) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(DocumentKind.Receipts) }
    var span by remember { mutableStateOf(DocumentSpan.Week) }
    var overview by remember { mutableStateOf<DocumentsOverview?>(null) }
    val list = remember { DocumentListState() }
    // Открытый документ показывается вместо списка: возвращаться к нему
    // владелец будет по «Закрыть», а не поиском своего места в списке.
    var opened by remember { mutableStateOf<OpenedDocument?>(null) }

    LaunchedEffect(registerId, kind, span, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        opened = null
        overview = cabinet.guard { cabinet.client.documentsOverview(token, registerId) }
        list.reset()
        list.loadNext(cabinet, token, registerId, kind, span, texts)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        DocumentCounters(overview, texts)
        ChoiceSegments(
            options = DocumentKind.entries,
            selected = kind,
            label = { it.title(texts) },
            onSelect = { kind = it }
        )
        DocumentSpanSegments(kind, span, texts) { span = it }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug)) {
            val document = opened
            if (document != null) {
                OpenedCard(document, texts) { opened = null }
                return@Column
            }
            DocumentList(list, kind, texts, onOpen = { row ->
                scope.launch { opened = openDocument(cabinet, registerId, kind, row) }
            }) {
                scope.launch {
                    val token = cabinet.token ?: return@launch
                    list.loadNext(cabinet, token, registerId, kind, span, texts)
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
 * Ряд крупных чисел без карточки вокруг: раздел и так внутри карточки
 * кассы, и вторая рамка вокруг четырёх чисел добавляла бы линий, а не
 * смысла.
 */
@Composable
private fun DocumentCounters(overview: DocumentsOverview?, texts: CabinetTexts) {
    val counts = overview ?: return
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        if (list.rows.isEmpty()) {
            EmptyState(AppIcons.history, texts.documentsEmpty, texts.documentsEmptyHint)
            return@Column
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
