package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.ui.adaptive.TableColumn
import kz.mybrain.superkassa.desktop.ui.adaptive.TableLine
import kz.mybrain.superkassa.desktop.ui.adaptive.TableWidths
import kz.mybrain.superkassa.desktop.ui.adaptive.tableWidths
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Таблица аналитики, которая живёт не сама по себе, а внутри страницы.
 *
 * Общая таблица ([kz.mybrain.superkassa.desktop.ui.adaptive.ScrollingTable])
 * сама прокручивает свои строки и потому занимает всю высоту. В аналитике
 * таблицы стоят внутри прокручиваемой страницы — сводка торговли — или
 * строками внутри общего ленивого списка вкладки — учёт касс. Столбцы
 * здесь те же: не уже своей наименьшей ширины, лишнее — по весам;
 * не хватает места — строки едут вбок все вместе, одним сдвигом.
 *
 * @property widths ширины столбцов в данном месте.
 * @property across общий сдвиг вбок для всех строк таблицы.
 * @property wide таблица шире места и прокручивается вбок.
 */
@Stable
internal class TableAcross(val widths: TableWidths, val across: ScrollState, val wide: Boolean)

/** Ширины столбцов для места [room] и сдвиг вбок, общий для всех строк. */
@Composable
internal fun rememberTableAcross(columns: List<TableColumn>, room: Dp): TableAcross {
    val widths = TableWidths(columns, tableWidths(columns, room))
    return TableAcross(widths, rememberScrollState(), widths.total > room)
}

/**
 * Строка таблицы, которая едет вбок вместе с остальными строками.
 *
 * Для строк, разложенных по ленивому списку: у каждой свой узел прокрутки,
 * но сдвиг у всех один, и столбцы не расходятся.
 */
@Composable
internal fun AcrossLine(
    table: TableAcross,
    modifier: Modifier = Modifier,
    cell: @Composable BoxScope.(Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(table.across)) {
        TableLine(table.widths, modifier, cell)
    }
}

/** Полоса прокрутки вбок под таблицей; пока таблица в месте помещается, её нет. */
@Composable
internal fun AcrossBar(table: TableAcross) {
    if (!table.wide) return
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(table.across),
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.hairline)
    )
}

/** Черта между строками — во всю ширину таблицы, а не только видимой части. */
@Composable
internal fun LineDivider(widths: TableWidths) {
    HorizontalDivider(modifier = Modifier.width(widths.total), color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * Таблица целиком внутри прокручиваемой страницы: все строки сразу.
 *
 * Для коротких таблиц — страница строк, свод по регионам: высота у них
 * своя, и страница прокручивает их вместе с остальным.
 */
@Composable
internal fun PageTable(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(TableAcross) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val table = rememberTableAcross(columns, maxWidth)
        Column {
            Column(modifier = Modifier.horizontalScroll(table.across)) { content(table) }
            AcrossBar(table)
        }
    }
}
