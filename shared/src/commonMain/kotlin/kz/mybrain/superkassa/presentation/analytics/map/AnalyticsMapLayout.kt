package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kz.mybrain.superkassa.presentation.common.adaptive.NarrowPanes
import kz.mybrain.superkassa.presentation.common.adaptive.TwoPane
import kz.mybrain.superkassa.presentation.common.keyboard.scrolledByKeys
import kz.mybrain.superkassa.presentation.common.list.ColumnScrollbar
import kz.mybrain.superkassa.presentation.theme.size.AnalyticsLayout
import kz.mybrain.superkassa.presentation.theme.size.Panes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Ряды отбора сверху и карта под ними — на всю оставшуюся высоту.
 *
 * В низком окне ряды отбора переносятся и растут, а карта под ними
 * сжималась до сотни точек и пропадала вовсе. Здесь карта берёт всё,
 * что осталось, но не меньше [AnalyticsLayout.mapLeast]; не хватает
 * окна и на это — прокручивается весь раздел, а карта остаётся картой.
 * Колесо над самой картой по-прежнему приближает её: карта его съедает.
 */
@Composable
internal fun HeadOverMap(
    modifier: Modifier = Modifier,
    head: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier = modifier) {
        val room = constraints.maxHeight
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .scrolledByKeys(scroll) { scroll.viewportSize },
            content = {
                Box { head() }
                Box { body() }
            }
        ) { measurables, constraints ->
            val gap = Spacing.fieldGap.roundToPx()
            val width = constraints.maxWidth
            val top = measurables[0].measure(Constraints(minWidth = width, maxWidth = width))
            val rest = maxOf(room - top.height - gap, AnalyticsLayout.mapLeast.roundToPx())
            val bottom = measurables[1].measure(Constraints.fixed(width, rest))
            layout(width, top.height + gap + rest) {
                top.place(0, 0)
                bottom.place(0, top.height + gap)
            }
        }
        ColumnScrollbar(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

/**
 * Карта и то, что о ней рассказывает: список касс и карточка выбранной.
 *
 * Рядом, пока обеим хватает места ([Panes.mapAndDetails]); иначе — одна
 * над другой, и карта сверху. Прежде карточка кассы стояла под картой
 * и отнимала у неё высоту: выбрав кассу в малом окне, владелец терял карту.
 * Теперь карточка живёт рядом со списком, и карта всегда во всю высоту
 * своей панели.
 */
@Composable
internal fun MapAndDetails(
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
    list: @Composable () -> Unit,
    card: @Composable () -> Unit
) {
    TwoPane(
        split = Panes.mapAndDetails,
        modifier = modifier,
        narrow = NarrowPanes.Stacked,
        first = map,
        second = { ListOverCard(list = list, card = card) }
    )
}

/**
 * Список касс, а под ним карточка выбранной.
 *
 * Карточка берёт свою высоту, но не больше [Panes.STACKED_SECOND_SHARE]
 * панели, и прокручивается сама: в малом окне «Аналитика кассы» уходила
 * за нижний край, и открыть её было нечем. Остальное — списку. Карточка
 * под списком, а не над ним: выбранная строка не уезжает из-под указателя,
 * когда карточка раскрывается.
 */
@Composable
internal fun ListOverCard(
    modifier: Modifier = Modifier,
    list: @Composable () -> Unit,
    card: @Composable () -> Unit
) {
    val scroll = rememberScrollState()
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box { list() }
            Box(modifier = Modifier.verticalScroll(scroll).padding(end = Spacing.scrollbarGutter)) { card() }
            ColumnScrollbar(scroll, Modifier)
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val cap = (height * Panes.STACKED_SECOND_SHARE).toInt()
        val lower = measurables[1].measure(Constraints(minWidth = width, maxWidth = width, maxHeight = cap))
        val gap = if (lower.height > 0) Spacing.fieldGap.roundToPx() else 0
        val upper = measurables[0].measure(Constraints.fixed(width, (height - lower.height - gap).coerceAtLeast(0)))
        val bar = measurables[2].measure(Constraints.fixedHeight(lower.height))
        layout(width, height) {
            upper.place(0, 0)
            lower.place(0, height - lower.height)
            bar.place(width - bar.width, height - lower.height)
        }
    }
}
