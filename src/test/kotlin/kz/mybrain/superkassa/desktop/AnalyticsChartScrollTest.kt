package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.ui.analytics.SalesBar
import kz.mybrain.superkassa.desktop.ui.analytics.SalesChart
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Колесо над графиком прокручивает страницу, а не застревает на нём.
 *
 * Сводка длиннее экрана, а графики стоят в её середине: перехвати полотно
 * колесо — и владельцу пришлось бы уводить мышь к краю окна, чтобы
 * дочитать сводку. Вертикальной прокрутки у графика нет, и колесо он
 * обязан пропускать дальше.
 *
 * Меряется не картинка, а ход прокрутки: картинка меняется и от одного
 * наведения — под указателем загорается столбик и появляется его значение.
 */
class AnalyticsChartScrollTest {

    private val texts = analyticsTexts(Language.Ru).sales

    private fun bars() = (1..WEEK).map {
        SalesBar(label = "0$it.09", caption = "сутки $it", value = BigDecimal(it * 1000))
    }

    @Composable
    private fun Page(onState: (ScrollState) -> Unit) {
        val scroll = rememberScrollState()
        onState(scroll)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            SectionCard(texts.byDay) { SalesChart(bars(), texts) }
            repeat(TAIL) { Text("строка $it") }
        }
    }

    /** Насколько уехала страница от колеса в этой точке. */
    private fun scrolledAt(at: Offset): Int {
        var state: ScrollState? = null
        RenderProbe { Page { state = it } }.use { probe ->
            probe.frame()
            probe.wheel(at = at, ticks = TICKS)
        }
        return state?.value ?: -1
    }

    @Test
    fun `колесо над графиком прокручивает страницу так же, как мимо него`() {
        val besideChart = scrolledAt(Offset(POINT_X, BELOW_CHART))
        val overChart = scrolledAt(Offset(POINT_X, OVER_CHART))
        println("прокрутка над графиком: $overChart px, ниже графика: $besideChart px")
        assertTrue(besideChart > 0, "страница не прокрутилась вовсе — проверять нечего")
        assertTrue(overChart > 0, "колесо над графиком не прокрутило страницу")
    }

    @Test
    fun `наведение на столбик показывает его значение`() {
        RenderProbe { Page {} }.use { probe ->
            val untouched = probe.frame()
            probe.wheel(at = Offset(POINT_X, OVER_CHART), ticks = 0f)
            assertTrue(probe.changedFrom(untouched), "наведение на столбик ничего не изменило")
        }
    }

    private companion object {
        const val WEEK = 7
        const val TAIL = 60
        const val TICKS = 8f
        const val POINT_X = 590f

        /** Точка посреди полотна: заголовок раздела, строка значения и столбики под ними. */
        const val OVER_CHART = 160f

        /** Точка ниже графика — на строках хвоста, где прокрутка работала всегда. */
        const val BELOW_CHART = 700f
    }
}
