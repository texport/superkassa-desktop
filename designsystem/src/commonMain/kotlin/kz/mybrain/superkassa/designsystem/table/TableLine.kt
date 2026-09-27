package kz.mybrain.superkassa.designsystem.table

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.keyboard.scrolledByKeys
import kz.mybrain.superkassa.designsystem.theme.size.TableColumns

/** Заголовок и строки с прибитыми столбцами: вбок едет каждая строка сама, общей прокруткой. */
@Composable
internal fun PinnedBody(
    widths: TableWidths,
    state: LazyListState,
    header: @Composable (TableWidths) -> Unit,
    rows: LazyListScope.(TableWidths) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        header(widths)
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
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
    val across = widths.across
    if (across == null) {
        Row(modifier = modifier.width(widths.total), verticalAlignment = Alignment.CenterVertically) {
            Cells(widths, widths.widths.indices, cell)
        }
        return
    }
    val moving = widths.widths.size - widths.pinned
    Row(modifier = modifier.width(widths.viewport), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).horizontalScroll(across), verticalAlignment = Alignment.CenterVertically) {
            Cells(widths, 0 until moving, cell)
        }
        Cells(widths, moving until widths.widths.size, cell)
    }
}

/** Ячейки столбцов [range]: число — вправо, остальное — влево. */
@Composable
private fun Cells(widths: TableWidths, range: IntRange, cell: @Composable BoxScope.(Int) -> Unit) {
    range.forEach { index ->
        Box(
            modifier = Modifier.width(widths.widths[index]).padding(horizontal = TableColumns.cellPadding),
            contentAlignment = if (widths.columns[index].numeric) Alignment.CenterEnd else Alignment.CenterStart
        ) { cell(index) }
    }
}
