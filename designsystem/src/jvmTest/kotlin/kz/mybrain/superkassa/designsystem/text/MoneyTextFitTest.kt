package kz.mybrain.superkassa.designsystem.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Сумма не рвётся посреди числа и не обрезается.
 *
 * Правило владельца: в суммах обрезки не бывает никогда. Сумма от
 * миллиарда в узкой ячейке переносилась посреди разрядов. Замер — высота
 * суммы: одна строка своей ступени, одна строка уменьшенной — но никогда
 * не две. Для сравнения — обычный `Text` в той же ширине: он переносится.
 *
 * Кадры остаются в `/tmp/money-fit-*.png`.
 */
class MoneyTextFitTest {

    /** Сумма от миллиарда тенге, как её пишет касса: разряды неразрывным пробелом. */
    private val billion = "98 797 031 109,45 ₸".replace(' ', Glyphs.NBSP)

    /** Высота и ширина того, что нарисовалось в ячейке заданной ширины. */
    private fun measured(cell: Int, name: String, content: @Composable () -> Unit): Pair<Int, Int> {
        var size = 0 to 0
        val frame = RenderProbe(width = WINDOW, height = HEIGHT) {
            Box(modifier = Modifier.width(cell.dp)) {
                Box(modifier = Modifier.onGloballyPositioned { size = it.size.height to it.size.width }) { content() }
            }
        }.use { it.frame() }
        File("/tmp/money-fit-$name-$cell.png").writeBytes(frame)
        return size
    }

    @Test
    fun `ступень выбирается первая поместившаяся, а без места — самая малая`() {
        assertEquals(0, moneyStep(listOf(300, 250, 200), room = 320))
        assertEquals(1, moneyStep(listOf(300, 250, 200), room = 260))
        assertEquals(2, moneyStep(listOf(300, 250, 200), room = 200))
        assertEquals(2, moneyStep(listOf(300, 250, 200), room = 40))
    }

    @Test
    fun `сумма от миллиарда остаётся одной строкой в любой ширине`() {
        val (roomy, roomyWidth) = measured(ROOMY, "own") { MoneyText(billion, style = MoneyStyle.hero) }
        // Ячейка чуть уже своей ступени: уменьшенная в неё уже помещается.
        val cell = (roomyWidth * SHORT_OF_OWN).toInt()
        val (reduced, reducedWidth) = measured(cell, "reduced") { MoneyText(billion, style = MoneyStyle.hero) }
        val (tight, _) = measured(TIGHT, "tight") { MoneyText(billion, style = MoneyStyle.hero) }
        val (plain, _) = measured(TIGHT, "plain") { Text(billion, style = MoneyStyle.hero) }
        println("сумма: $ROOMY → $roomy×$roomyWidth, $cell → $reduced×$reducedWidth, $TIGHT → $tight; Text — $plain")
        assertTrue(roomy in 1..ONE_LINE, "своей ступенью сумма заняла $roomy px — это не одна строка")
        assertTrue(reduced < roomy, "в тесной ячейке сумма не перешла на уменьшенную ступень")
        assertTrue(reducedWidth <= cell, "уменьшенная сумма шире ячейки: $reducedWidth > $cell")
        assertTrue(tight in 1..roomy, "в самой узкой ячейке сумма заняла $tight px — перенеслась")
        assertTrue(plain > roomy, "обычный текст в той же ширине не перенёсся — сравнение неверно")
    }

    private companion object {
        const val WINDOW = 800
        const val HEIGHT = 300

        /** Ячейка, в которой сумма стоит своей ступенью. */
        const val ROOMY = 600

        /** Доля своей ширины у ячейки, где своей ступени мало, а уменьшенной — хватает. */
        const val SHORT_OF_OWN = 0.93f

        /** Ячейка уже любой ступени. */
        const val TIGHT = 120

        /** Высота одной строки крупной суммы при плотности 1. */
        const val ONE_LINE = 48
    }
}
