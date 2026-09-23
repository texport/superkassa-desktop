package kz.mybrain.superkassa.presentation.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.presentation.theme.PaneSplit
import kz.mybrain.superkassa.presentation.theme.Panes

/** Что делают две панели, когда рядом им тесно. */
sealed interface NarrowPanes {

    /**
     * Одна над другой: вторая — своей высоты, но не больше
     * [Panes.STACKED_SECOND_SHARE] высоты; остальное — первой.
     * Для пары, где обе части нужны сразу: чек и касса.
     */
    data object Stacked : NarrowPanes

    /**
     * По одной: показана вторая, если [showSecond], иначе первая.
     * Для пары «список и подробности»: возврат к списку — забота экрана.
     */
    data class Switched(val showSecond: Boolean) : NarrowPanes
}

/** Ширины панелей рядом или `null`, если рядом они не помещаются. */
internal fun sideBySide(split: PaneSplit, room: Dp, gap: Dp): Pair<Dp, Dp>? {
    val usable = room - gap
    if (usable < split.firstMin + split.secondMin) return null
    val second = (usable * (1f - split.firstShare))
        .coerceIn(split.secondMin, minOf(split.secondMax, usable - split.firstMin))
    return (usable - second) to second
}

/**
 * Две панели: рядом на просторном месте, по-другому — на тесном.
 *
 * Решает не класс окна, а место, которое досталось самой раскладке:
 * одно и то же окно с развёрнутым и свёрнутым рельсом оставляет экрану
 * разную ширину, и панели обязаны встать по месту, а не по монитору.
 * Рядом они стоят, пока каждой хватает её наименьшей ширины из [split];
 * доли и пределы — там же, общими токенами.
 *
 * Переход между раскладками не теряет состояния панелей: набранное
 * в поле и прокрутка списка переживают растягивание окна через порог.
 *
 * @param narrow что делать, когда рядом тесно.
 */
@Composable
fun TwoPane(
    split: PaneSplit,
    modifier: Modifier = Modifier,
    narrow: NarrowPanes = NarrowPanes.Stacked,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    val firstNow by rememberUpdatedState(first)
    val secondNow by rememberUpdatedState(second)
    val firstKept = remember { movableContentOf { firstNow() } }
    val secondKept = remember { movableContentOf { secondNow() } }
    BoxWithConstraints(modifier = modifier) {
        val widths = sideBySide(split, maxWidth, Panes.gap)
        when {
            widths != null -> Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(widths.first).fillMaxHeight()) { firstKept() }
                Spacer(modifier = Modifier.width(Panes.gap))
                Box(modifier = Modifier.width(widths.second).fillMaxHeight()) { secondKept() }
            }
            narrow is NarrowPanes.Switched -> Box(modifier = Modifier.fillMaxSize()) {
                if (narrow.showSecond) secondKept() else firstKept()
            }
            else -> Stacked(firstKept, secondKept)
        }
    }
}

/**
 * Одна над другой: вторая берёт свою высоту в пределах доли,
 * первая — всё, что осталось, и сама прокручивает лишнее.
 *
 * Высота у раскладки обязана быть ограничена — внутри прокрутки делить
 * нечего. Потому `TwoPane` ставится вместо прокрутки экрана, а не в неё.
 */
@Composable
private fun Stacked(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box { first() }
            Box { second() }
        }
    ) { measurables, constraints ->
        val gap = Panes.gap.roundToPx()
        val width = constraints.maxWidth
        val cap = (constraints.maxHeight * Panes.STACKED_SECOND_SHARE).toInt()
        val lower = measurables[1].measure(Constraints(minWidth = width, maxWidth = width, maxHeight = cap))
        val rest = (constraints.maxHeight - lower.height - gap).coerceAtLeast(0)
        val upper = measurables[0].measure(Constraints.fixed(width, rest))
        layout(width, constraints.maxHeight) {
            upper.place(0, 0)
            lower.place(0, rest + gap)
        }
    }
}
