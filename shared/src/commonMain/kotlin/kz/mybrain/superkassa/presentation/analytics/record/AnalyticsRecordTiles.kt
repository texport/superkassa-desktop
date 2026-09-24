package kz.mybrain.superkassa.presentation.analytics.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.status.StatusTone
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.analytics.model.RecordCount
import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.presentation.cabinet.recordTitle
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsRecordTexts
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.fill

/**
 * Парк касс числами: сколько их всего и что с ними у КГД.
 *
 * Первым идёт число всего парка — это и есть главное число вкладки,
 * и набрано оно шкалой `display`. За ним четыре смысла учёта, и цвет
 * у них не украшение: на учёте — благополучие, учёт идёт — ожидание,
 * отказ — работа владельца, снято — покой. Дальше числа второго
 * вопроса: где эти кассы стоят и работают ли прямо сейчас.
 *
 * Своих рамок у чисел нет: они стоят на карточке раздела, и рамка
 * вокруг каждого добавила бы к ней вторую границу.
 */
@Composable
fun RecordTiles(count: RecordCount, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    val record = texts.record
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        TileRow {
            MainTile(count.total, record.total)
            // Смыслы учёта перечисляются сами — порядок плиток и порядок
            // столбцов таблицы под ними задан одним и тем же списком.
            KkmRecord.entries.forEach { meaning ->
                RecordTile(count.of(meaning), recordTitle(meaning, texts.sieve), recordTone(meaning))
            }
        }
        TileRow {
            RecordTile(count.places, record.places, StatusTone.Idle)
            TradingTile(count, record)
            RecordTile(count.blocked, record.blocked, StatusTone.Bad)
        }
    }
}

/** Ряд плиток: переносится, а не сжимается — числу парка нужна вся его ширина. */
@Composable
private fun TileRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        content = content
    )
}

/** Главное число вкладки: весь парк касс компании. */
@Composable
private fun MainTile(value: Int, label: String) {
    Tile(Money.count(value), label, MaterialTheme.typography.displaySmall, MaterialTheme.colorScheme.onSurface)
}

/** Число со смыслом: цвет берётся у общих состояний приложения, см. [recordPaint]. */
@Composable
private fun RecordTile(value: Int, label: String, tone: StatusTone) {
    Tile(Money.count(value), label, MaterialTheme.typography.headlineMedium, recordPaint(value, tone))
}

/**
 * Сколько касс торгует прямо сейчас — и из скольких стоящих на учёте.
 *
 * Одним числом эта плитка обманывала: «Сейчас торгуют 0» рядом с «Всего
 * касс 3294» читается как остановившаяся сеть, хотя торговать вправе
 * четыре кассы из этих трёх тысяч. Основание счёта стоит тут же, в самом
 * числе, и спорить с ним нечему.
 */
@Composable
private fun TradingTile(count: RecordCount, texts: AnalyticsRecordTexts) {
    Tile(
        value = texts.tradingOf.fill(Money.count(count.trading), Money.count(count.onRecord)),
        label = texts.trading,
        style = MaterialTheme.typography.headlineMedium,
        tone = recordPaint(count.trading, StatusTone.Good)
    )
}

/** Одна плитка: число крупно, подпись под ним. */
@Composable
private fun Tile(value: String, label: String, style: TextStyle, tone: Color) {
    Column(
        modifier = Modifier.widthIn(min = Sizes.counterTile),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        Text(text = value, style = style, color = tone, maxLines = 1)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
