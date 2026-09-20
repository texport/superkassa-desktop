package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.ui.components.MoreRow
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сводка по кассам или по точкам таблицей.
 *
 * Одна таблица на оба разреза: считается в них одно и то же, а разные
 * заголовки не повод писать её дважды. Столбцы при этом зависят от того,
 * что в строках, — их набор объявлен в [salesColumns].
 *
 * Строки показываются десятками: у сети их бывают сотни, а столбец
 * в сотню строк внутри прокручиваемого экрана заставляет искать конец
 * таблицы колесом. Сколько показано из скольких — сказано под таблицей,
 * рядом с кнопкой, которая это меняет.
 *
 * @param kind что в строках: кассы или торговые точки.
 */
@Composable
fun SalesTable(
    rows: List<SalesUnit>,
    kind: SalesRows,
    texts: AnalyticsTexts,
    journal: HistoryJournalTexts,
    allShown: String,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) {
        Footnote(texts.sales.empty)
        return
    }
    var sort: SalesSort by remember { mutableStateOf(SalesSort()) }
    var shown: Int by remember(rows) { mutableIntStateOf(PAGE) }
    val sorted = sortedUnits(rows, sort)
    val page = sorted.take(shown)
    val columns = salesColumns(kind)
    Column(modifier = modifier.fillMaxWidth()) {
        SalesHead(columns, kind, texts, journal, sort) { sort = sort.toggled(it) }
        page.forEach { row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SalesRow(columns, kind, row)
        }
        TableFooter(page.size, sorted.size, journal, allShown) { shown += PAGE }
    }
}

/** Низ таблицы: сколько строк показано из скольких и чем это изменить. */
@Composable
private fun TableFooter(
    shown: Int,
    total: Int,
    journal: HistoryJournalTexts,
    allShown: String,
    onMore: () -> Unit
) {
    MoreRow(
        more = total > shown,
        loading = false,
        showMore = journal.showMore,
        allShown = allShown,
        note = "${journal.shown}: $shown / $total",
        onMore = onMore
    )
}

/** Подписи столбцов; по числовым таблица и выстраивается. */
@Composable
private fun SalesHead(
    columns: List<SalesColumn>,
    kind: SalesRows,
    texts: AnalyticsTexts,
    journal: HistoryJournalTexts,
    sort: SalesSort,
    onSort: (SalesOrder) -> Unit
) {
    TableRow {
        columns.forEach { column ->
            val title = salesColumnTitle(column, kind, texts)
            val order = salesSortOrder(column)
            if (order == null) {
                HeadCell(title, cellWidth(column, kind))
            } else {
                SortCell(title, order, sort, journal, onSort)
            }
        }
    }
}

/** Строка сводки: чем торговали и когда эта касса выходила на связь. */
@Composable
private fun SalesRow(columns: List<SalesColumn>, kind: SalesRows, row: SalesUnit) {
    TableRow(Modifier.padding(vertical = Spacing.tight)) {
        columns.forEach { column ->
            val value = salesCellValue(column, row)
            if (salesMoneyColumn(column)) {
                MoneyCell(value)
            } else {
                RowCell(value, cellWidth(column, kind))
            }
        }
    }
}

/** Строка таблицы: та же раскладка у заголовка и у значений. */
@Composable
private fun TableRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Ширина столбца.
 *
 * Числовые и время стоят на месте — по ним таблицу читают сверху вниз
 * и сравнивают строки между собой. Словесные тянутся: в таблице касс их
 * три и ширина делится между ними, в таблице точек — один, и он забирает
 * всё, что освободилось от снятых столбцов.
 */
@Composable
private fun RowScope.cellWidth(column: SalesColumn, kind: SalesRows): Modifier = when {
    column == SalesColumn.Name && kind == SalesRows.Registers -> Modifier.width(Sizes.salesNameColumn)
    column == SalesColumn.Name -> Modifier.weight(1f)
    column == SalesColumn.RegistrationNumber || column == SalesColumn.RetailPlace -> Modifier.weight(1f)
    column == SalesColumn.LastContact -> Modifier.width(Sizes.exchangeMomentColumn)
    else -> Modifier.width(Sizes.salesNumberColumn)
}

/** Подпись столбца, которая и выстраивает таблицу; стрелка — куда именно. */
@Composable
private fun SortCell(
    title: String,
    column: SalesOrder,
    sort: SalesSort,
    journal: HistoryJournalTexts,
    onSort: (SalesOrder) -> Unit
) {
    TextButton(
        onClick = { onSort(column) },
        modifier = Modifier.width(Sizes.salesNumberColumn),
        contentPadding = PaddingValues(Spacing.hairline)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (sort.by == column) {
            Icon(
                imageVector = if (sort.descending) AppIcons.descending else AppIcons.ascending,
                contentDescription = if (sort.descending) journal.descending else journal.ascending,
                modifier = Modifier.size(Sizes.chipIcon)
            )
        }
    }
}

/** Сумма в строке: моноширинная и по правому краю, как во всём приложении. */
@Composable
private fun MoneyCell(value: String) {
    Text(
        text = value,
        style = MoneyStyle.caption,
        maxLines = 1,
        modifier = Modifier.width(Sizes.salesNumberColumn)
    )
}

/** Строк в одном показе таблицы. */
private const val PAGE = 10
