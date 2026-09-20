package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.theme.ChartColors
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Столбики сводки: выручка по дням и нагрузка по часам.
 *
 * Полотно, а не готовая библиотека графиков: приложению нужны ровно два
 * столбчатых ряда, а всякая библиотека приносит своё оформление, свои
 * шрифты и свои цвета — и спорит со схемой кассы в тёмной теме. Рисуется
 * так же, как знаки на карте: цвета сняты со схемы в показе и отданы
 * рисованию готовыми, потому что полотно до схемы не дотягивается.
 *
 * Значение читается при наведении: подписать сотню столбиков суммами
 * нельзя, а подпись у одного — того, на который смотрят, — отвечает
 * на вопрос «сколько именно здесь» без единого лишнего элемента.
 */
@Composable
fun SalesChart(bars: List<SalesBar>, texts: AnalyticsSalesTexts, modifier: Modifier = Modifier) {
    if (bars.none { it.value.signum() > 0 }) {
        Footnote(texts.nothingToDraw)
        return
    }
    var hovered: Int? by remember(bars) { mutableStateOf(null) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        ChartReadout(hovered?.let { bars[it].caption }.orEmpty())
        ChartCanvas(bars, hovered) { hovered = it }
        ChartAxis(bars)
    }
}

/**
 * Само полотно.
 *
 * Цвета и размеры снимаются со схемы здесь, в показе, и уходят
 * в рисование готовыми: полотно рисует вне композиции и ни до схемы,
 * ни до плотности экрана оттуда не дотягивается.
 */
@Composable
private fun ChartCanvas(bars: List<SalesBar>, hovered: Int?, onHover: (Int?) -> Unit) {
    val paint = BarPaint(ChartColors.bar, ChartColors.barChosen, ChartColors.axis)
    val gap = Sizes.chartBarGap
    val axis = Sizes.chartAxis
    val top = bars.maxOf { it.value }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.chartHeight)
            .pointerInput(bars.size) { watchHover(bars.size, onHover) }
    ) {
        drawBars(bars, Chart(top, hovered, gap.toPx(), axis.toPx()), paint)
    }
}

/**
 * Что показывает столбик под указателем.
 *
 * Без указателя строка пуста, но место своё держит: «Наведите на столбик»
 * объясняло очевидное и занимало строку всегда, а прыгающий при наведении
 * график читается хуже пустой строки.
 */
@Composable
private fun ChartReadout(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        minLines = 1,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Ось времени под столбиками.
 *
 * Подписан не каждый столбик, а каждый восьмой: тридцать дат подряд
 * слипаются в серую полосу, из которой не прочесть ни одной.
 */
@Composable
private fun ChartAxis(bars: List<SalesBar>) {
    val step = axisStep(bars.size)
    Row(modifier = Modifier.fillMaxWidth()) {
        bars.forEachIndexed { at, bar ->
            Box(modifier = Modifier.weight(1f)) {
                if (at % step == 0) {
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/** Служебная строка вместо графика, когда рисовать нечего. */
@Composable
internal fun Footnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Цвета столбиков, снятые со схемы до рисования. */
private data class BarPaint(val bar: Color, val chosen: Color, val axis: Color)

/** Что нужно рисованию, кроме самих столбиков. */
private data class Chart(val top: BigDecimal, val chosen: Int?, val gap: Float, val axis: Float)

/** Столбики и ось под ними. */
private fun DrawScope.drawBars(bars: List<SalesBar>, chart: Chart, paint: BarPaint) {
    val slot = size.width / bars.size
    val width = (slot - chart.gap).coerceAtLeast(1f)
    bars.forEachIndexed { at, bar ->
        val height = size.height * barShare(bar.value, chart.top)
        if (height <= 0f) return@forEachIndexed
        drawRect(
            color = if (at == chart.chosen) paint.chosen else paint.bar,
            topLeft = Offset(at * slot + chart.gap / 2, size.height - height),
            size = Size(width, height)
        )
    }
    drawRect(
        color = paint.axis,
        topLeft = Offset(0f, size.height - chart.axis),
        size = Size(size.width, chart.axis)
    )
}

/**
 * Столбик под указателем.
 *
 * Считается делением ширины на число столбиков, а не перебором
 * их границ: промахнуться мимо трёхточечного просвета между соседями
 * нельзя, и при наведении всегда назван ровно один столбик.
 *
 * Колесо полотно не трогает вовсе: вертикальной прокрутки у графика нет,
 * а страница под ним длинная — перехвати он колесо, владельцу пришлось бы
 * уводить мышь к краю окна, чтобы прокрутить сводку дальше.
 */
private suspend fun PointerInputScope.watchHover(count: Int, onHover: (Int?) -> Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) continue
            val at = event.changes.lastOrNull()?.position
            val gone = event.type == PointerEventType.Exit
            onHover(if (gone) null else at?.let { barAt(it.x, size.width, count) })
        }
    }
}
