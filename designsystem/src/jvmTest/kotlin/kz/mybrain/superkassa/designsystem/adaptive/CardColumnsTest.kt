package kz.mybrain.superkassa.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.size.CardGrid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Карточки во всю ширину: столбцов столько, сколько позволяют класс окна,
 * число карточек и ширина места, а карточка встаёт в самый короткий столбец.
 */
class CardColumnsTest {

    /** Левый край и ширина каждой карточки после раскладки [count] карточек в месте шириной [room]. */
    private fun layout(
        window: Int,
        room: Int,
        count: Int,
        heights: List<Int> = List(count) { CARD }
    ): List<Pair<Int, Int>> {
        val placed = MutableList(count) { 0 to 0 }
        RenderProbe(width = window, height = HEIGHT) {
            Box(modifier = Modifier.width(room.dp)) {
                CardColumns(Modifier.fillMaxWidth()) {
                    heights.forEachIndexed { at, height ->
                        Box(
                            Modifier.height(height.dp).onGloballyPositioned {
                                placed[at] = it.positionInRoot().x.toInt() to it.size.width
                            }
                        )
                    }
                }
            }
        }.use { it.frame() }
        return placed
    }

    private val gap = CardGrid.gap.value.toInt()

    @Test
    fun `на узком окне карточки стоят одним столбцом во всю ширину`() {
        val cards = layout(window = NARROW, room = NARROW, count = 3)
        assertEquals(List(3) { 0 to NARROW }, cards)
    }

    @Test
    fun `на очень большом окне три карточки делят ширину на три столбца`() {
        val cards = layout(window = WIDE, room = ROOM, count = 3)
        val column = (ROOM - 2 * gap) / 3
        assertEquals(listOf(0 to column, column + gap to column, 2 * (column + gap) to column), cards)
    }

    @Test
    fun `две карточки на очень большом окне делят ширину пополам, третьего пустого столбца нет`() {
        val cards = layout(window = WIDE, room = ROOM, count = 2)
        val column = (ROOM - gap) / 2
        assertEquals(listOf(0 to column, column + gap to column), cards)
    }

    @Test
    fun `в узкой панели широкого окна столбцов столько, сколько помещается`() {
        val room = (CardGrid.columnMin.value * 2).toInt() + gap
        val cards = layout(window = WIDE, room = room, count = 3)
        assertEquals(2, cards.map { it.first }.distinct().size, "в панели $room: $cards")
    }

    /**
     * Порядок читается по столбцам сверху вниз и не зависит от высоты.
     *
     * Прежде карточка вставала в самый короткий столбец: шаги читались
     * «1, 4 / 2, 3», а нажатие, менявшее высоту одной карточки,
     * переставляло соседние.
     */
    @Test
    fun `карточки идут по столбцам по порядку, и высота их не переставляет`() {
        val tall = layout(window = MEDIUM_WIDE, room = ROOM, count = 3, heights = listOf(TALL, CARD, CARD))
        val even = layout(window = MEDIUM_WIDE, room = ROOM, count = 3, heights = listOf(CARD, CARD, CARD))

        assertEquals(tall.map { it.first }, even.map { it.first }, "высота переставила карточки по столбцам")
        assertEquals(tall[0].first, tall[1].first, "вторая карточка ушла из первого столбца: $tall")
        assertTrue(tall[2].first > tall[1].first, "третья карточка не во втором столбце: $tall")
    }

    private companion object {
        const val NARROW = 420
        const val MEDIUM_WIDE = 1280
        const val WIDE = 2000
        const val ROOM = 1440
        const val HEIGHT = 900
        const val CARD = 100
        const val TALL = 400
    }
}
