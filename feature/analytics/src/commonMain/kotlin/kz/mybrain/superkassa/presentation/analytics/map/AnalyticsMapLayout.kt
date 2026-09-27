package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import kz.mybrain.superkassa.designsystem.adaptive.TwoPane
import kz.mybrain.superkassa.designsystem.keyboard.scrolledByKeys
import kz.mybrain.superkassa.designsystem.list.ColumnScrollbar
import kz.mybrain.superkassa.designsystem.list.besideEdge
import kz.mybrain.superkassa.designsystem.theme.size.AnalyticsLayout
import kz.mybrain.superkassa.designsystem.theme.size.Panes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

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
        ColumnScrollbar(scroll, Modifier.align(Alignment.CenterEnd).fillMaxHeight().besideEdge())
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
    card: @Composable () -> Unit,
    listOpen: Boolean = true
) {
    TwoPane(
        split = Panes.mapAndDetails,
        modifier = modifier,
        first = map,
        second = { ListOverCard(listOpen = listOpen, list = list, card = card) }
    )
}

/**
 * Список касс, а под ним карточка выбранной — одной ширины, одна над другой.
 *
 * Карточка берёт свою высоту и прокручивается сама, но не выше предела:
 * раскрыт список — половина колонки, свёрнут — всё до его нижнего края.
 * Прежде предел был долей колонки при любом списке, и свёрнутый список
 * оставлял над карточкой пустоту, а раскрытая карточка в низком окне
 * уходила низом за экран. Карточка под списком, а не над ним: выбранная
 * строка не уезжает из-под указателя, когда карточка раскрывается.
 *
 * Полоса прокрутки карточки — только там, где она есть: на Android
 * и iOS её показывает сама прокрутка пальцем, и третьей части у раскладки
 * нет. Прежде раскладка всегда брала третью часть и на планшете падала
 * при открытии аналитики.
 *
 * Список берёт остаток высоты, свёрнутый — только свой заголовок.
 *
 * @param listOpen раскрыт ли список касс.
 * @param scrollbar полоса прокрутки карточки; на Android и iOS — пустая.
 */
@Composable
internal fun ListOverCard(
    modifier: Modifier = Modifier,
    listOpen: Boolean = true,
    list: @Composable () -> Unit,
    card: @Composable () -> Unit,
    scrollbar: @Composable (ScrollState) -> Unit = { ColumnScrollbar(it, Modifier) }
) {
    val scroll = rememberScrollState()
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box { list() }
            Box(modifier = Modifier.verticalScroll(scroll)) { card() }
            scrollbar(scroll)
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gap = Spacing.fieldGap.roundToPx()
        val (upper, lower) = stack(listOpen, width, height, gap, measurables[0], measurables[1])
        // Полосы прокрутки на Android и iOS нет вовсе — узла под неё тоже нет.
        val bar = measurables.getOrNull(2)?.measure(Constraints.fixedHeight(lower.height))
        layout(width, height) {
            upper.place(0, 0)
            lower.place(0, height - lower.height)
            bar?.place(width - bar.width + Spacing.scrollbarOutset.roundToPx(), height - lower.height)
        }
    }
}

/**
 * Список и карточка по правилу колонки: раскрытый список делит её
 * с карточкой пополам и берёт остаток, свёрнутый отдаёт карточке всё ниже
 * своего заголовка.
 */
private fun stack(
    listOpen: Boolean,
    width: Int,
    height: Int,
    gap: Int,
    list: Measurable,
    card: Measurable
): Pair<Placeable, Placeable> = if (listOpen) {
    val lower = card.measure(column(width, (height - gap) / 2))
    list.measure(column(width, (height - lower.height - gap).coerceAtLeast(0))) to lower
} else {
    val upper = list.measure(column(width, height))
    upper to card.measure(column(width, (height - upper.height - gap).coerceAtLeast(0)))
}

/** Ограничения части столбца: вся его ширина и не выше [height]. */
private fun column(width: Int, height: Int) = Constraints(minWidth = width, maxWidth = width, maxHeight = height)
