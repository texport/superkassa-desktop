package kz.mybrain.superkassa.presentation.common.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.common.keyboard.scrolledByKeys
import kz.mybrain.superkassa.presentation.common.list.ListScrollbar
import kz.mybrain.superkassa.presentation.common.list.RowScrollbar
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.theme.size.TableColumns

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

/**
 * Ширины столбцов, посчитанные по месту; [total] — ширина всей строки.
 *
 * @param pinned сколько последних столбцов стоит на месте, пока остальные
 *   едут вбок.
 * @param across прокрутка вбок, общая для всех строк, когда столбцы
 *   прибиты; `null` — строка едет вбок целиком.
 * @param viewport ширина видимой части таблицы: столько занимает строка
 *   с прибитыми столбцами.
 */
@Immutable
data class TableWidths(
    val columns: List<TableColumn>,
    val widths: List<Dp>,
    val pinned: Int = 0,
    val across: ScrollState? = null,
    val viewport: Dp = Dp(0f)
) {
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
 * не хватает окна — таблица едет вбок, и на настольной кассе видны обе
 * полосы прокрутки.
 * Заголовок едет вбок вместе со строками и стоит на месте при прокрутке
 * вниз.
 *
 * Кнопки в конце строки можно прибить ([pinned]): тогда едут вбок только
 * данные, а кнопки строки видны всегда — как у таблиц Material 3 с действиями
 * в конце. Без этого в окне 1180 точек кнопка печати стояла за краем
 * наполовину, и что строку можно напечатать, было видно только прокрутив.
 *
 * @param pinned сколько последних столбцов стоит на месте; строки в этом
 *   случае собираются только [TableLine].
 * @param header строка заголовка; ширины — те же, что у строк.
 * @param rows строки: каждую удобно собрать [TableLine].
 */
@Composable
fun ScrollingTable(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    pinned: Int = 0,
    header: @Composable (TableWidths) -> Unit,
    rows: LazyListScope.(TableWidths) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val across = rememberScrollState()
        val room = maxWidth - Spacing.normal
        val sized = tableWidths(columns, room)
        if (pinned > 0) {
            PinnedBody(TableWidths(columns, sized, pinned, across, room), state, header, rows)
        } else {
            TableBody(TableWidths(columns, sized), across, state, header, rows)
        }
        ListScrollbar(state, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        RowScrollbar(across, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
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
