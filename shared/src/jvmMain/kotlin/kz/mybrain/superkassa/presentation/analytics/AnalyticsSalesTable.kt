package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kz.mybrain.superkassa.data.cabinet.SalesUnit
import kz.mybrain.superkassa.presentation.adaptive.NumberText
import kz.mybrain.superkassa.presentation.adaptive.TableColumn
import kz.mybrain.superkassa.presentation.adaptive.TableLine
import kz.mybrain.superkassa.presentation.adaptive.TableWidths
import kz.mybrain.superkassa.presentation.components.MoreRow
import kz.mybrain.superkassa.presentation.strings.AnalyticsTexts
import kz.mybrain.superkassa.presentation.strings.HistoryJournalTexts
import kz.mybrain.superkassa.presentation.theme.AnalyticsLayout
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.Sizes
import kz.mybrain.superkassa.presentation.theme.Spacing
import kz.mybrain.superkassa.presentation.theme.TableColumns

/**
 * Сводка по кассам или по точкам таблицей.
 *
 * Одна таблица на оба разреза: считается в них одно и то же, а разные
 * заголовки не повод писать её дважды. Столбцы при этом зависят от того,
 * что в строках, — их набор объявлен в [salesColumns].
 *
 * Столбцы не уже своей наименьшей ширины; не хватает окна — таблица едет
 * вбок, а номер КГД и суммы не обрываются. Числа — вправо, с разрядами.
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
        PageTable(columns.map { tableColumn(it, kind) }) { table ->
            SalesHead(table.widths, columns, kind, texts, journal, sort) { sort = sort.toggled(it) }
            page.forEach { row ->
                LineDivider(table.widths)
                SalesRow(table.widths, columns, row)
            }
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
    widths: TableWidths,
    columns: List<SalesColumn>,
    kind: SalesRows,
    texts: AnalyticsTexts,
    journal: HistoryJournalTexts,
    sort: SalesSort,
    onSort: (SalesOrder) -> Unit
) {
    TableLine(widths, Modifier.padding(vertical = Spacing.hairline)) { index ->
        val column = columns[index]
        val title = salesColumnTitle(column, kind, texts)
        val order = salesSortOrder(column)
        if (order == null) {
            HeadCell(title, numeric = salesNumeric(column))
        } else {
            SortCell(title, order, sort, journal, onSort)
        }
    }
}

/** Строка сводки: чем торговали и когда эта касса выходила на связь. */
@Composable
private fun SalesRow(widths: TableWidths, columns: List<SalesColumn>, row: SalesUnit) {
    TableLine(widths, Modifier.padding(vertical = Spacing.tight)) { index ->
        val column = columns[index]
        val value = salesCellValue(column, row)
        when {
            salesMoneyColumn(column) -> SumCell(value)
            salesNumeric(column) -> NumberText(value)
            else -> RowCell(value)
        }
    }
}

/**
 * Столбец таблицы по смыслу.
 *
 * Числовые и время стоят на месте — по ним таблицу читают сверху вниз
 * и сравнивают строки между собой. Словесные тянутся: в таблице касс их
 * два и ширина делится между ними, в таблице точек — один, и он забирает
 * всё, что освободилось от снятых столбцов. Сумме отведён столбец суммы:
 * в прежней ширине миллиарды тенге теряли тиыны за краем.
 */
private fun tableColumn(column: SalesColumn, kind: SalesRows): TableColumn = when (column) {
    SalesColumn.Name -> TableColumn(min = TableColumns.name, weight = if (kind == SalesRows.Registers) 1f else 2f)
    SalesColumn.RetailPlace -> TableColumn(min = TableColumns.name)
    SalesColumn.RegistrationNumber -> TableColumn(min = TableColumns.number, weight = 0f, numeric = true)
    SalesColumn.Receipts -> TableColumn(min = TableColumns.count, weight = NUMBER_SHARE, numeric = true)
    SalesColumn.Revenue, SalesColumn.Net -> TableColumn(min = AnalyticsLayout.networkSum, weight = NUMBER_SHARE, numeric = true)
    SalesColumn.LastContact -> TableColumn(min = TableColumns.moment, weight = 0f)
}

/**
 * Доля лишнего места у чисел — вдвое меньше, чем у названия.
 *
 * Лишнее достаётся и счёту, и суммам: у сети на миллиарды тенге сумма
 * на просторе встаёт своей ступенью, а не уменьшенной.
 */
private const val NUMBER_SHARE = 0.5f

/** Число, а не слово: номер КГД, чеки и суммы стоят вправо, моноширинно. */
private fun salesNumeric(column: SalesColumn): Boolean =
    column == SalesColumn.RegistrationNumber || column == SalesColumn.Receipts || salesMoneyColumn(column)

/** Подпись столбца, которая и выстраивает таблицу; стрелка — куда именно. */
@Composable
private fun SortCell(
    title: String,
    column: SalesOrder,
    sort: SalesSort,
    journal: HistoryJournalTexts,
    onSort: (SalesOrder) -> Unit
) {
    TextButton(onClick = { onSort(column) }, contentPadding = PaddingValues(Spacing.hairline)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.End)
        if (sort.by == column) {
            Icon(
                imageVector = if (sort.descending) AppIcons.descending else AppIcons.ascending,
                contentDescription = if (sort.descending) journal.descending else journal.ascending,
                modifier = Modifier.size(Sizes.chipIcon)
            )
        }
    }
}

/** Строк в одном показе таблицы. */
private const val PAGE = 10
