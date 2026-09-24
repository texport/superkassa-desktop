package kz.mybrain.superkassa.designsystem.text

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.isSpecified
import kz.mybrain.superkassa.designsystem.theme.size.MoneyFit
import kz.mybrain.superkassa.designsystem.theme.size.NumberStyle
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle

/**
 * Денежная сумма, которая не рвётся и не обрезается.
 *
 * Правило владельца: в суммах обрезки не бывает никогда. Сумма от
 * миллиарда переносилась посреди числа — «98 797 031 109,» на одной строке,
 * тиыны на другой — или молча теряла хвост за краем ячейки.
 * Здесь сумма всегда одной строкой и целиком: своей ступенью, а если
 * не помещается — заметно уменьшенной ([MoneyFit]). Если места нет и для
 * самой малой ступени, сумма всё равно рисуется целиком, выходя за край
 * влево, — лучше наезд на соседа, чем неверное число.
 *
 * Наименьшая собственная ширина — ширина самой малой ступени: столбец,
 * который меряет содержимое, отдаст сумме хотя бы её.
 *
 * @param text сумма, уже набранная `Money.format` или `Money.formatTiyn`.
 * @param style своя ступень суммы; по умолчанию — строка списка.
 */
@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MoneyStyle.row,
    color: Color = LocalContentColor.current
) {
    WholeLine(text, modifier, style, color)
}

/**
 * Число в таблице: номер документа, смены, счёт чеков.
 *
 * То же правило, что у суммы, — число целиком одной строкой, при нехватке
 * места уменьшенной ступенью; набрано моноширинно и прижато вправо.
 *
 * @param text число, уже разбитое на разряды (`Money.count`), если это счёт.
 */
@Composable
fun NumberText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NumberStyle.cell,
    color: Color = LocalContentColor.current
) {
    WholeLine(text, modifier, style, color)
}

/** Строка, которая встаёт целиком одной из ступеней [MoneyFit] и никогда не переносится. */
@Composable
private fun WholeLine(text: String, modifier: Modifier, style: TextStyle, color: Color) {
    Layout(
        modifier = modifier,
        content = {
            MoneyFit.steps.forEach { factor ->
                Text(
                    text = text,
                    style = style.stepped(factor),
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        },
        measurePolicy = MoneyFitPolicy
    )
}

/** Ступень суммы: кегль и высота строки в один множитель. */
private fun TextStyle.stepped(factor: Float): TextStyle = copy(
    fontSize = fontSize * factor,
    lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight
)

/** Какая ступень суммы встаёт в ширину: первая поместившаяся, иначе самая малая. */
internal fun moneyStep(widths: List<Int>, room: Int): Int =
    widths.indexOfFirst { it <= room }.takeIf { it >= 0 } ?: widths.lastIndex

/**
 * Меряет все ступени во всю их ширину и ставит одну.
 *
 * Сумма прижата к правому краю, как в столбце сумм: не поместившаяся
 * выходит за левый край, а не теряет тиыны справа.
 */
private object MoneyFitPolicy : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val placeables = measurables.map { it.measure(Constraints()) }
        val chosen = placeables[moneyStep(placeables.map { it.width }, constraints.maxWidth)]
        val width = chosen.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = chosen.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) { chosen.place(width - chosen.width, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int = measurables.last().maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int = measurables.first().maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int = measurables.last().maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int = measurables.first().maxIntrinsicHeight(width)
}
