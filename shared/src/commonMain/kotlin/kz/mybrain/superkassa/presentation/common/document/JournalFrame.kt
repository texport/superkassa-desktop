package kz.mybrain.superkassa.presentation.common.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kz.mybrain.superkassa.presentation.common.keyboard.scrolledByKeys
import kz.mybrain.superkassa.presentation.common.list.ColumnScrollbar
import kz.mybrain.superkassa.presentation.theme.size.HistoryLayout
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kotlin.math.roundToInt

/**
 * Отбор над строками, который не вытесняет строки.
 *
 * В окне 960×640 срок, поиск и три ряда плашек занимали всю высоту
 * журнала, и строк не оставалось ни одной. Здесь отбор берёт свою
 * высоту, но не больше [HistoryLayout.HEAD_SHARE] от места журнала;
 * длиннее — прокручивается сам, с видимой полосой. Строки получают
 * всё остальное. В просторном окне отбор помещается целиком, и разницы
 * с обычной колонкой нет.
 *
 * @param head срок, поиск и плашки отбора.
 * @param body таблица или то, что стоит на её месте.
 */
@Composable
fun JournalFrame(
    modifier: Modifier = Modifier,
    head: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    Layout(
        contents = listOf(
            { Head(head) },
            { Column(verticalArrangement = Arrangement.spacedBy(Spacing.snug), content = body) }
        ),
        modifier = modifier
    ) { (heads, bodies), constraints ->
        val gap = Spacing.snug.roundToPx()
        val cap = (constraints.maxHeight * HistoryLayout.HEAD_SHARE).roundToInt()
        val top = heads.first().measure(constraints.copy(minHeight = 0, maxHeight = cap))
        val rest = (constraints.maxHeight - top.height - gap).coerceAtLeast(0)
        val rows = bodies.first().measure(Constraints.fixed(constraints.maxWidth, rest))
        layout(constraints.maxWidth, constraints.maxHeight) {
            top.place(0, 0)
            rows.place(0, top.height + gap)
        }
    }
}

/**
 * Отбор с прокруткой, ростом по содержимому.
 *
 * Собран из тех же частей, что `ScrollableColumn`: полоса видна всегда,
 * клавиатура листает. Отличие одно — высота: полоса здесь не распирает
 * блок до предела, и короткий отбор не оставляет под собой пустоты.
 */
@Composable
private fun Head(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .scrolledByKeys(scroll) { scroll.viewportSize }
                .padding(end = Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug),
            content = content
        )
        Box(modifier = Modifier.matchParentSize()) {
            ColumnScrollbar(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}
