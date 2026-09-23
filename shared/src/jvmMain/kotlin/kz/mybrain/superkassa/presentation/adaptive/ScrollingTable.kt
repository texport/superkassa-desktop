package kz.mybrain.superkassa.presentation.adaptive

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.components.scrolledByKeys
import kz.mybrain.superkassa.presentation.theme.Spacing
import kz.mybrain.superkassa.presentation.theme.TableColumns

/**
 * Столбец таблицы.
 *
 * @param min уже этого столбец не бывает — токен из [TableColumns].
 * @param weight доля лишнего места, когда окно шире суммы наименьших;
 *   0 — столбец своей наименьшей ширины всегда (кнопка в конце строки).
 * @param numeric число: прижато вправо, как столбец сумм.
 */
@Immutable
data class TableColumn(val min: Dp, val weight: Float = 1f, val numeric: Boolean = false)

/** Ширины столбцов, посчитанные по месту; [total] — ширина всей строки. */
@Immutable
data class TableWidths(val columns: List<TableColumn>, val widths: List<Dp>) {
    val total: Dp = widths.fold(Dp(0f)) { sum, width -> sum + width }
}

/**
 * Ширины столбцов в данном месте.
 *
 * Места больше суммы наименьших — лишнее делится по весам; меньше —
 * каждый столбец своей наименьшей ширины, а таблица прокручивается вбок.
 */
internal fun tableWidths(columns: List<TableColumn>, room: Dp): List<Dp> {
    val least = columns.fold(Dp(0f)) { sum, column -> sum + column.min }
    val weights = columns.sumOf { it.weight.toDouble() }.toFloat()
    if (room <= least || weights <= 0f) return columns.map { it.min }
    val extra = room - least
    return columns.map { it.min + extra * (it.weight / weights) }
}

/**
 * Таблица, которая не сжимает столбцы, а прокручивается.
 *
 * В узком окне у журнала не было ни минимальной ширины столбцов,
 * ни прокрутки: кнопка печати пропадала до точки, а сумма слипалась
 * с признаком. Здесь столбец не бывает уже своей наименьшей ширины;
 * не хватает окна — таблица едет вбок, и обе полосы прокрутки видны.
 * Заголовок едет вбок вместе со строками и стоит на месте при прокрутке
 * вниз.
 *
 * @param header строка заголовка; ширины — те же, что у строк.
 * @param rows строки: каждую удобно собрать [TableLine].
 */
@Composable
fun ScrollingTable(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: @Composable (TableWidths) -> Unit,
    rows: LazyListScope.(TableWidths) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widths = TableWidths(columns, tableWidths(columns, maxWidth - Spacing.normal))
        val across = rememberScrollState()
        TableBody(widths, across, state, header, rows)
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(across),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        )
    }
}

/**
 * Заголовок и строки, которые едут вбок вместе.
 *
 * Справа за последним столбцом — поле под полосу прокрутки: доехав
 * до края, кнопка строки не прячется под полосу.
 */
@Composable
private fun TableBody(
    widths: TableWidths,
    across: ScrollState,
    state: LazyListState,
    header: @Composable (TableWidths) -> Unit,
    rows: LazyListScope.(TableWidths) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().horizontalScroll(across).padding(end = Spacing.normal)) {
        Box(modifier = Modifier.width(widths.total)) { header(widths) }
        LazyColumn(
            state = state,
            modifier = Modifier
                .width(widths.total)
                .weight(1f)
                .scrolledByKeys(state) { state.layoutInfo.viewportSize.height },
            content = { rows(widths) }
        )
    }
}

/**
 * Строка таблицы: ячейки ровно по ширинам столбцов.
 *
 * Числовая ячейка прижата вправо, остальные — влево; поля ячейки
 * одни на все таблицы, и столбцы не слипаются.
 *
 * @param cell содержимое ячейки по номеру столбца.
 */
@Composable
fun TableLine(
    widths: TableWidths,
    modifier: Modifier = Modifier,
    cell: @Composable BoxScope.(Int) -> Unit
) {
    Row(modifier = modifier.width(widths.total), verticalAlignment = Alignment.CenterVertically) {
        widths.widths.forEachIndexed { index, width ->
            Box(
                modifier = Modifier.width(width).padding(horizontal = TableColumns.cellPadding),
                contentAlignment = if (widths.columns[index].numeric) Alignment.CenterEnd else Alignment.CenterStart
            ) { cell(index) }
        }
    }
}
